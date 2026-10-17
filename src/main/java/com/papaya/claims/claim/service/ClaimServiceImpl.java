package com.papaya.claims.claim.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.claim.domain.*;
import com.papaya.claims.audit.Auditable;
import com.papaya.claims.common.web.Traceparent;
import com.papaya.claims.workflow.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

/** Transactional application boundary coordinating claim, evidence, audit and durable effects. */
@Service
public class ClaimServiceImpl implements ClaimUseCases {
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final Pattern CORRELATION_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final Pattern DOCUMENT_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,119}");
    private static final Set<String> TOP_LEVEL_FACTS = Set.of("documents", "assessorId", "assessmentReport");
    private static final Set<String> HUMAN_ROLES = Set.of("member", "document_clerk", "team_lead", "assessor", "finance", "claims_admin");

    private final ClaimRepository claims;
    private final ClaimAuditRepository claimAudit;
    private final SystemOperationAuditRepository systemAudit;
    private final IdempotencyRepository idempotency;
    private final PaymentRepository payments;
    private final OutboxRepository outbox;
    private final WorkflowEngine workflow;
    private final WorkflowDefinition definition;
    private final Clock clock;
    private final Duration appealPeriod;
    private final Duration dispatchDelay;
    private final ObjectMapper json;

    public ClaimServiceImpl(ClaimRepository claims, ClaimAuditRepository claimAudit,
            SystemOperationAuditRepository systemAudit, IdempotencyRepository idempotency,
            PaymentRepository payments, OutboxRepository outbox, WorkflowEngine workflow,
            WorkflowDefinition definition, Clock clock, Duration appealPeriod, ObjectMapper json,
            @Value("${claims.outbox.dispatch-delay-ms:1000}") long dispatchDelayMillis) {
        this.claims = claims;
        this.claimAudit = claimAudit;
        this.systemAudit = systemAudit;
        this.idempotency = idempotency;
        this.payments = payments;
        this.outbox = outbox;
        this.workflow = workflow;
        this.definition = definition;
        this.clock = clock;
        this.appealPeriod = appealPeriod;
        if (dispatchDelayMillis < 0) throw new IllegalArgumentException("Outbox dispatch delay cannot be negative");
        this.dispatchDelay = Duration.ofMillis(dispatchDelayMillis);
        this.json = json.copy().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    @Override
    @Transactional
    @Auditable(action = "claim.create", resource = "claim")
    public Claim create(CreateClaimCommand command, Actor actor, String key, String correlationId) {
        requireHuman(actor);
        requireRole(actor, "member");
        String scope = "claims:create";
        String fingerprint = fingerprint(Map.of("policyLimit", command.policyLimit(), "currency", command.currency(),
                "facts", command.facts().values()));
        idempotency.lock(actor.id(), scope, requireKey(key));
        Optional<Claim> replay = replay(actor.id(), scope, key, fingerprint, Claim.class);
        if (replay.isPresent()) return replay.get();
        Instant now = now();
        validateRequestedAmount(command.facts().values().get("requestedAmount"));
        if (!command.facts().values().keySet().stream().allMatch("requestedAmount"::equals))
            throw forbidden("Only requestedAmount may be supplied when a claim is created");
        BigDecimal amount = requestedAmountDecimal(command.facts().values().get("requestedAmount"));
        if (amount == null || amount.compareTo(command.policyLimit()) > 0)
            throw invalid("requestedAmount must be present and no greater than the policy limit");
        // Preserve a decimal point through JSONB so whole-dollar values deserialize as BigDecimal.
        amount = amount.setScale(2);
        ClaimFacts normalizedFacts = new ClaimFacts(Map.of("requestedAmount", amount));
        Claim claim = new Claim(UUID.randomUUID(), actor.id(), definition.initialState(), command.policyLimit(),
                command.currency(), 0, normalizedFacts, now, now);
        claims.insert(new ClaimAggregate(claim, ClaimWorkflowData.empty()));
        writeSystemAudit("claim.create", "claim", claim.id().toString(), actor, correlationId, "success", null);
        storeReceipt(actor.id(), scope, key, fingerprint, claim);
        return claim;
    }

    @Override
    @Transactional(readOnly = true)
    public Claim get(UUID claimId, Actor actor) {
        return visibleClaim(claimId, actor, false).claim();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowOption> options(UUID claimId, Actor actor) {
        ClaimAggregate aggregate = visibleClaim(claimId, actor, false);
        return workflow.available(new WorkflowEvaluationContext(aggregate.claim(), actor,
                new TransitionCommand("__options__", aggregate.claim().version(), "", new ClaimFacts(Map.of())),
                evidence(aggregate.workflow(), claimId), now(), appealPeriod));
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResult<Claim> listVisible(PageRequest page, Actor actor) {
        requireActor(actor);
        PageResult<ClaimAggregate> result = claims.listVisible(page, actor);
        return new PageResult<>(result.items().stream().map(ClaimAggregate::claim).toList(), result.total(), page);
    }

    @Override
    @Transactional
    @Auditable(action = "claim.facts.update", resource = "claim")
    public Claim updateFacts(UUID claimId, UpdateFactsCommand command, Actor actor, String key, String correlationId) {
        requireHuman(actor);
        String scope = "claims:" + claimId + ":facts";
        String fingerprint = fingerprint(Map.of("expectedVersion", command.expectedVersion(), "facts", command.facts().values()));
        lockAndCheckKey(actor, scope, key);
        Optional<Claim> replay = replay(actor.id(), scope, key, fingerprint, Claim.class);
        if (replay.isPresent()) return replay.get();
        ClaimAggregate current = lockedClaim(claimId, actor);
        checkVersion(current.claim(), command.expectedVersion());
        ClaimFacts merged = mergeFacts(current.claim(), command.facts(), actor);
        ClaimWorkflowData data = current.workflow();
        if (actor.hasRole("member") && current.claim().state().equals("PENDING_INFO")) {
            data = new ClaimWorkflowData(data.informationRequests(), data.lastInformationRequestedAt(), now(),
                    data.assessmentStartedAt(), data.rejectedAt(), data.memberAcknowledgedRejectionAt());
        }
        Claim updated = replace(current.claim(), current.claim().state(), merged, current.claim().version() + 1, now());
        if (!claims.compareAndSet(new ClaimAggregate(updated, data), command.expectedVersion())) throw versionConflict();
        writeSystemAudit("claim.facts.update", "claim", claimId.toString(), actor, correlationId, "success", null);
        storeReceipt(actor.id(), scope, key, fingerprint, updated);
        return updated;
    }

    @Override
    @Transactional
    @Auditable(action = "claim.rejection.acknowledge", resource = "claim")
    public Claim acknowledgeRejection(UUID claimId, long expectedVersion, Actor actor, String key, String correlationId) {
        requireHuman(actor);
        requireRole(actor, "member");
        String scope = "claims:" + claimId + ":rejection-acknowledgement";
        String fingerprint = fingerprint(Map.of("expectedVersion", expectedVersion, "action", "acknowledge-rejection"));
        lockAndCheckKey(actor, scope, key);
        Optional<Claim> replay = replay(actor.id(), scope, key, fingerprint, Claim.class);
        if (replay.isPresent()) return replay.get();
        ClaimAggregate current = lockedClaim(claimId, actor);
        checkVersion(current.claim(), expectedVersion);
        if (!current.claim().state().equals("REJECTED"))
            throw new BusinessException("INVALID_STATE", "Only a rejected claim can be acknowledged", 409);
        Instant acknowledgedAt = now();
        ClaimWorkflowData old = current.workflow();
        ClaimWorkflowData data = new ClaimWorkflowData(old.informationRequests(), old.lastInformationRequestedAt(),
                old.lastInformationReceivedAt(), old.assessmentStartedAt(), old.rejectedAt(), acknowledgedAt);
        Claim updated = replace(current.claim(), current.claim().state(), current.claim().facts(),
                current.claim().version() + 1, acknowledgedAt);
        if (!claims.compareAndSet(new ClaimAggregate(updated, data), expectedVersion)) throw versionConflict();
        writeSystemAudit("claim.rejection.acknowledge", "claim", claimId.toString(), actor, correlationId, "SUCCESS", null);
        storeReceipt(actor.id(), scope, key, fingerprint, updated);
        return updated;
    }

    @Override
    @Transactional
    @Auditable(action = "claim.transition", resource = "claim")
    public TransitionResult transition(UUID claimId, TransitionCommand command, Actor actor,
            String key, String correlationId) {
        requireHuman(actor);
        return doTransition(claimId, command, actor, key, correlationId, false);
    }

    @Override
    @Transactional
    @Auditable(action = "claim.system-transition", resource = "claim")
    public TransitionResult systemTransition(UUID claimId, TransitionCommand command, Actor actor,
            String key, String correlationId) {
        requireActor(actor);
        if (!actor.system() || !actor.hasRole("system")) throw forbidden("Protected system identity is required");
        return doTransition(claimId, command, actor, key, correlationId, true);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResult<AuditRecord> audit(UUID claimId, PageRequest page, Actor actor) {
        visibleClaim(claimId, actor, false);
        return claimAudit.findByClaim(claimId, page);
    }

    private TransitionResult doTransition(UUID claimId, TransitionCommand command, Actor actor,
            String key, String correlationId, boolean systemCall) {
        String scope = "claims:" + claimId + ":transition";
        String fingerprint = fingerprint(Map.of("targetState", command.targetState(),
                "expectedVersion", command.expectedVersion(), "reason", command.reason(),
                "facts", command.suppliedFacts().values(), "system", systemCall));
        lockAndCheckKey(actor, scope, key);
        Optional<TransitionResult> replay = replay(actor.id(), scope, key, fingerprint, TransitionResult.class);
        if (replay.isPresent()) {
            TransitionResult result = replay.get();
            return new TransitionResult(result.claim(), result.audit(), true);
        }
        ClaimAggregate current = lockedClaim(claimId, actor);
        checkVersion(current.claim(), command.expectedVersion());
        ClaimFacts merged = command.suppliedFacts().values().isEmpty()
                ? current.claim().facts() : mergeFacts(current.claim(), command.suppliedFacts(), actor);
        Claim candidate = replace(current.claim(), current.claim().state(), merged,
                current.claim().version(), current.claim().updatedAt());
        WorkflowEvidence evidence = evidence(current.workflow(), claimId);
        WorkflowDecision decision = workflow.evaluate(new WorkflowEvaluationContext(candidate, actor, command,
                evidence, now(), appealPeriod));
        if (!decision.allowed()) throw new BusinessException(decision.failureCode(), decision.failureMessage(),
                decision.failureCode().equals("ROLE_FORBIDDEN") ? 403 : 409);

        Instant occurred = now();
        ClaimWorkflowData nextData = updateWorkflowData(current.workflow(), decision, occurred);
        ClaimFacts nextFacts = clearStaleAssessmentFacts(merged, decision);
        Claim updated = replace(candidate, decision.to(), nextFacts, current.claim().version() + 1, occurred);
        if (!claims.compareAndSet(new ClaimAggregate(updated, nextData), command.expectedVersion())) throw versionConflict();

        String role = matchingRole(actor, decision.transitionId());
        AuditRecord audit = new AuditRecord(UUID.randomUUID(), claimId, updated.version(), occurred,
                decision.from(), decision.to(), actor.id(), role, command.reason(), safeCorrelation(correlationId), definition.version());
        claimAudit.append(audit);
        persistEffects(decision.effects(), claimId, candidate);
        writeSystemAudit(systemCall ? "claim.system-transition" : "claim.transition", "claim", claimId.toString(),
                actor, correlationId, "success", null);
        TransitionResult result = new TransitionResult(updated, audit, false);
        storeReceipt(actor.id(), scope, key, fingerprint, result);
        return result;
    }

    private void persistEffects(List<EffectIntent> intents, UUID claimId, Claim claim) {
        Instant available = now().plus(dispatchDelay);
        for (EffectIntent intent : intents) {
            if (intent.id().equals("create_payment_request")) {
                Object rawAmount = claim.facts().values().get("requestedAmount");
                if (!(rawAmount instanceof BigDecimal amount)) throw invalid("Requested amount must be an exact decimal");
                payments.create(claimId, UUID.randomUUID(), claim.currency(), amount);
            }
            outbox.enqueue(new OutboxRepository.Message(UUID.randomUUID(), claimId, intent.id(),
                    writeJson(intent.payload()), 0, available, Traceparent.current()));
        }
    }

    private ClaimWorkflowData updateWorkflowData(ClaimWorkflowData old, WorkflowDecision decision, Instant now) {
        int requests = old.informationRequests();
        Instant requested = old.lastInformationRequestedAt();
        Instant assessment = old.assessmentStartedAt();
        Instant rejected = old.rejectedAt();
        Instant acknowledged = old.memberAcknowledgedRejectionAt();
        for (EffectIntent effect : decision.effects()) {
            switch (effect.id()) {
                case "record_assessment_start" -> assessment = now;
                case "notify_information_request" -> { requests++; requested = now; }
                case "reset_assessment_timer" -> assessment = null;
                case "notify_member_rejection" -> { rejected = now; acknowledged = null; }
                default -> { }
            }
        }
        return new ClaimWorkflowData(requests, requested, old.lastInformationReceivedAt(), assessment, rejected, acknowledged);
    }

    private ClaimFacts clearStaleAssessmentFacts(ClaimFacts facts, WorkflowDecision decision) {
        if (decision.effects().stream().noneMatch(e -> e.id().equals("reset_assessment_timer"))) return facts;
        Map<String, Object> values = new LinkedHashMap<>(facts.values());
        values.remove("assessmentReport");
        return new ClaimFacts(values);
    }

    private WorkflowEvidence evidence(ClaimWorkflowData data, UUID claimId) {
        Optional<PaymentRepository.Confirmation> confirmation = payments.confirmation(claimId);
        return new WorkflowEvidence(data.informationRequests(), data.lastInformationRequestedAt(),
                data.lastInformationReceivedAt(), data.assessmentStartedAt(), data.rejectedAt(),
                data.memberAcknowledgedRejectionAt(), payments.exists(claimId),
                confirmation.map(PaymentRepository.Confirmation::reference).orElse(null),
                confirmation.map(PaymentRepository.Confirmation::confirmedAt).orElse(null));
    }

    private ClaimFacts mergeFacts(Claim current, ClaimFacts patch, Actor actor) {
        if (patch.values().isEmpty()) throw invalid("At least one fact must be supplied");
        Map<String, Object> merged = new LinkedHashMap<>(current.facts().values());
        for (var entry : patch.values().entrySet()) {
            String key = entry.getKey();
            if (!TOP_LEVEL_FACTS.contains(key)) throw forbidden("Claim fact is not writable: " + key);
            if (key.equals("documents")) merged.put(key, mergeDocuments(current, merged.get(key), entry.getValue(), actor));
            else if (key.equals("assessorId")) {
                if (!actor.hasRole("team_lead") || !Set.of("SUBMITTED", "DOCUMENTS_VERIFIED").contains(current.state()))
                    throw forbidden("Only a team lead may assign an assessor before assessment starts");
                if (!(entry.getValue() instanceof String s) || s.isBlank()) throw invalid("assessorId must be a nonblank string");
                merged.put(key, entry.getValue());
            } else {
                if (!actor.hasRole("assessor") || !current.state().equals("UNDER_ASSESSMENT")
                        || !Objects.equals(actor.id(), current.facts().values().get("assessorId")))
                    throw forbidden("Only the assigned assessor may submit an assessment report");
                if (!(entry.getValue() instanceof Map<?, ?> report) || !(report.get("complete") instanceof Boolean))
                    throw invalid("assessmentReport must contain a boolean complete field");
                merged.put(key, entry.getValue());
            }
        }
        return new ClaimFacts(merged);
    }

    private Map<String, Object> mergeDocuments(Claim claim, Object existingRaw, Object patchRaw, Actor actor) {
        if (!(patchRaw instanceof Map<?, ?> patch) || patch.isEmpty()) throw invalid("documents must be a nonempty object");
        Map<String, Object> existing = existingRaw instanceof Map<?, ?> map ? mutableMap(map) : new LinkedHashMap<>();
        Map<String, Object> result = new LinkedHashMap<>(existing);
        for (var entry : patch.entrySet()) {
            if (!(entry.getKey() instanceof String id) || !DOCUMENT_ID.matcher(id).matches()) throw invalid("Document ID is invalid");
            if (!(entry.getValue() instanceof Map<?, ?> rawFields) || rawFields.isEmpty()) throw invalid("Document fields must be a nonempty object");
            boolean member = actor.hasRole("member");
            boolean clerk = actor.hasRole("document_clerk");
            if (member && !claim.memberId().equals(actor.id())) throw forbidden("Member does not own this claim");
            if (member && !claim.state().equals("PENDING_INFO")) throw forbidden("Members may add information only while a claim is pending information");
            if (clerk && !Set.of("SUBMITTED", "PENDING_INFO").contains(claim.state())) throw forbidden("Documents cannot be updated in this claim state");
            if (!member && !clerk) throw forbidden("Only the claim member or document clerk may update documents");
            Object oldRaw = existing.get(id);
            boolean isNew = oldRaw == null;
            if (member && isNew) throw forbidden("Members cannot add document roster entries");
            if (isNew && !clerk) throw forbidden("Only a document clerk may add document roster entries");
            Map<String, Object> fields = oldRaw instanceof Map<?, ?> map ? mutableMap(map) : new LinkedHashMap<>();
            for (var field : rawFields.entrySet()) {
                if (!(field.getKey() instanceof String name) || !(field.getValue() instanceof Boolean)) throw invalid("Document fields must be booleans");
                if (member && !name.equals("present")) throw forbidden("Members may only update document presence");
                if (clerk && !Set.of("required", "present", "valid").contains(name)) throw invalid("Unknown document field: " + name);
                if (name.equals("required") && !isNew && !claim.state().equals("SUBMITTED")) throw forbidden("Existing document required status is immutable after submission");
                fields.put(name, field.getValue());
            }
            if (!(fields.get("required") instanceof Boolean) || !(fields.get("present") instanceof Boolean)
                    || !(fields.get("valid") instanceof Boolean)) throw invalid("Document entry must contain required, present and valid boolean fields");
            result.put(id, Map.copyOf(fields));
        }
        return Map.copyOf(result);
    }

    private static Map<String, Object> mutableMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((k, v) -> { if (k instanceof String key) result.put(key, v); });
        return result;
    }

    private ClaimAggregate lockedClaim(UUID id, Actor actor) {
        ClaimAggregate aggregate = claims.findForUpdate(id).orElseThrow(() -> notFound("Claim was not found"));
        requireVisible(aggregate.claim(), actor);
        return aggregate;
    }

    private ClaimAggregate visibleClaim(UUID id, Actor actor, boolean lock) {
        requireActor(actor);
        ClaimAggregate aggregate = (lock ? claims.findForUpdate(id) : claims.find(id))
                .orElseThrow(() -> notFound("Claim was not found"));
        requireVisible(aggregate.claim(), actor);
        return aggregate;
    }

    private static void requireVisible(Claim claim, Actor actor) {
        requireActor(actor);
        if (actor.hasRole("member") && !claim.memberId().equals(actor.id())) throw notFound("Claim was not found");
    }

    private void lockAndCheckKey(Actor actor, String scope, String key) {
        idempotency.lock(actor.id(), scope, requireKey(key));
    }

    private <T> Optional<T> replay(String actorId, String scope, String key, String fingerprint, Class<T> type) {
        IdempotencyRepository.Receipt receipt = idempotency.find(actorId, scope, key).orElse(null);
        if (receipt == null) return Optional.empty();
        if (!MessageDigest.isEqual(receipt.fingerprint().getBytes(StandardCharsets.US_ASCII), fingerprint.getBytes(StandardCharsets.US_ASCII)))
            throw new BusinessException("IDEMPOTENCY_CONFLICT", "Idempotency key was already used for a different request", 409);
        try { return Optional.of(json.readValue(receipt.resultJson(), type)); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Stored idempotency result is invalid", e); }
    }

    private void storeReceipt(String actor, String scope, String key, String fingerprint, Object result) {
        idempotency.insert(new IdempotencyRepository.Receipt(UUID.randomUUID(), actor, scope, key,
                fingerprint, writeJson(result)));
    }

    private void writeSystemAudit(String action, String resource, String resourceId, Actor actor,
            String correlationId, String outcome, String errorType) {
        systemAudit.append(new SystemOperationAuditRecord(UUID.randomUUID(), action, resource, resourceId,
                actor.id(), safeCorrelation(correlationId), outcome, errorType, now()));
    }

    private String fingerprint(Object request) {
        try {
            byte[] canonical = json.writeValueAsBytes(canonicalize(request));
            return HexFormatSupport.sha256(canonical);
        } catch (JsonProcessingException e) { throw new IllegalArgumentException("Could not fingerprint request", e); }
    }

    private Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((k, v) -> { if (k instanceof String key) sorted.put(key, canonicalize(v)); });
            return sorted;
        }
        if (value instanceof List<?> list) return list.stream().map(this::canonicalize).toList();
        return value;
    }

    private String writeJson(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Could not serialize persistence result", e); }
    }

    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private static Claim replace(Claim old, String state, ClaimFacts facts, long version, Instant updated) {
        return new Claim(old.id(), old.memberId(), state, old.policyLimit(), old.currency(), version, facts, old.createdAt(), updated);
    }

    private String matchingRole(Actor actor, String transitionId) {
        if (actor.system()) return "system";
        return definition.transitions().stream().filter(t -> t.id().equals(transitionId)).findFirst()
                .map(t -> t.roles().stream().filter(actor::hasRole).sorted().findFirst().orElse("unknown"))
                .orElse("unknown");
    }

    private static String requireKey(String key) {
        if (key == null || !IDEMPOTENCY_KEY.matcher(key).matches()) throw invalid("Idempotency-Key must contain 1 to 128 safe characters");
        return key;
    }
    private static String safeCorrelation(String value) {
        return value != null && CORRELATION_ID.matcher(value).matches() ? value : UUID.randomUUID().toString();
    }
    private static void checkVersion(Claim claim, long expected) { if (claim.version() != expected) throw versionConflict(); }
    private static BusinessException versionConflict() { return new BusinessException("VERSION_CONFLICT", "Claim version is stale", 409); }
    private static BusinessException notFound(String m) { return new BusinessException("NOT_FOUND", m, 404); }
    private static BusinessException forbidden(String m) { return new BusinessException("FORBIDDEN", m, 403); }
    private static BusinessException invalid(String m) { return new BusinessException("INVALID_REQUEST", m, 400); }
    private static void requireActor(Actor actor) { if (actor == null) throw forbidden("Authentication is required"); }
    private static void requireHuman(Actor actor) {
        requireActor(actor);
        if (actor.system()) throw forbidden("System identity cannot perform human operations");
        if (actor.roles().stream().noneMatch(HUMAN_ROLES::contains)) throw forbidden("Authenticated human role is required");
    }
    private static void requireRole(Actor actor, String role) { if (!actor.hasRole(role)) throw forbidden("Required role is missing"); }
    private static void validateRequestedAmount(Object amount) {
        if (amount == null) return;
        BigDecimal value = requestedAmountDecimal(amount);
        if (value == null || value.signum() < 0 || value.scale() > 2 || value.precision() - value.scale() > 16)
            throw invalid("requestedAmount must be a nonnegative exact decimal fitting NUMERIC(18,2)");
    }

    private static BigDecimal requestedAmountDecimal(Object amount) {
        if (amount instanceof BigDecimal decimal) return decimal;
        if (amount instanceof Number number) {
            try { return new BigDecimal(number.toString()); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private static final class HexFormatSupport {
        private static String sha256(byte[] content) {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
                StringBuilder result = new StringBuilder(64);
                for (byte b : digest) result.append(Character.forDigit((b >>> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
                return result.toString();
            } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 is unavailable", e); }
        }
    }
}
