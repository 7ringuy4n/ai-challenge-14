package com.papaya.claims.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionTemplate;

/** Coordinates domain decisions and all durable effects in a single database transaction. */
public final class ClaimService implements ClaimOperations {
    private static final Set<String> STAFF = Set.of("document_clerk", "team_lead", "assessor", "finance");
    private final ClaimRepository claims;
    private final AuditRepository audits;
    private final CommandRepository commands;
    private final EffectRepository effects;
    private final WorkflowEngine engine;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final MockEffectFactory effectFactory;
    private final EffectDispatcher dispatcher;
    private final ObjectMapper json;

    public ClaimService(ClaimRepository claims, AuditRepository audits, CommandRepository commands,
                        EffectRepository effects, WorkflowEngine engine, TransactionTemplate transactions,
                        Clock clock, MockEffectFactory effectFactory, EffectDispatcher dispatcher,
                        ObjectMapper json) {
        this.claims = claims; this.audits = audits; this.commands = commands; this.effects = effects;
        this.engine = engine; this.transactions = transactions; this.clock = clock;
        this.effectFactory = effectFactory; this.dispatcher = dispatcher; this.json = json;
    }

    @Override public ClaimSnapshot create(Actor actor, String memberId, BigDecimal limit, String currency,
                                           List<String> requiredDocuments) {
        Objects.requireNonNull(actor); requireAny(actor, Set.of("document_clerk", "team_lead", "member"));
        if ("member".equals(actor.role()) && !actor.userId().equals(memberId)) forbidden();
        Model.requireText(memberId, "memberId");
        if (memberId.length() > 120) invalid("memberId exceeds 120 characters");
        if (limit == null || limit.signum() < 0 || limit.scale() > 2) invalid("Policy limit must be nonnegative with at most two decimal places");
        validateCurrency(currency);
        if (requiredDocuments == null || requiredDocuments.isEmpty() || requiredDocuments.size() > 30) invalid("requiredDocuments must contain 1 to 30 document keys");
        var docs = new LinkedHashSet<String>();
        for (String doc : requiredDocuments) {
            Model.requireText(doc, "document key");
            if (doc.length() > 80 || !docs.add(doc)) invalid("Document keys must be unique and at most 80 characters");
        }
        UUID id = UUID.randomUUID();
        ClaimSnapshot claim = new ClaimSnapshot(id, memberId, engine.initialState(), limit, currency,
                Map.of("requiredDocuments", List.copyOf(docs), "documents", Map.of()), 0, 0, null, null);
        return transactions.execute(status -> { claims.insert(claim); return claim; });
    }

    @Override public List<ClaimSnapshot> list(Actor actor) {
        requireReadable(actor);
        return claims.list().stream().filter(c -> canRead(actor, c)).toList();
    }
    @Override public ClaimSnapshot get(Actor actor, UUID id) {
        ClaimSnapshot claim = claims.find(Objects.requireNonNull(id)).orElseThrow(ClaimService::notFound);
        requireRead(actor, claim); return claim;
    }

    @Override public ClaimSnapshot patchFacts(Actor actor, UUID id, long expectedVersion, Map<String,Object> updates) {
        Objects.requireNonNull(actor); Objects.requireNonNull(updates);
        ClaimSnapshot result = transactions.execute(status -> {
            ClaimSnapshot current = locked(id);
            requireRead(actor, current);
            if (terminal(current.state())) conflict("Terminal claims cannot be changed");
            if (current.version() != expectedVersion) conflict("Claim version is stale");
            Map<String,Object> accepted = validateUpdates(actor, current, updates);
            var merged = new LinkedHashMap<>(current.facts()); merged.putAll(accepted);
            ClaimSnapshot next = current.updateFacts(merged);
            if (!claims.compareAndSet(next, current.version())) conflict("Claim version is stale");
            return next;
        });
        return result;
    }

    @Override public ClaimSnapshot transition(Actor actor, UUID id, TransitionCommand command) {
        Objects.requireNonNull(actor); Objects.requireNonNull(command);
        AtomicResult outcome = transactions.execute(status -> {
            ClaimSnapshot current = locked(id); requireRead(actor, current);
            if ("member".equals(actor.role())) forbidden();
            if ("system".equals(actor.role()) && !("REJECTED".equals(current.state()) && "CLOSED".equals(command.target()))) forbidden();
            String hash = hash(command);
            Optional<CommandReceipt> prior = commands.find(id, actor.userId(), command.idempotencyKey());
            if (prior.isPresent()) {
                if (!MessageDigest.isEqual(prior.get().requestHash().getBytes(StandardCharsets.US_ASCII), hash.getBytes(StandardCharsets.US_ASCII)))
                    conflict("Idempotency key was already used with a different command");
                return new AtomicResult(prior.get().result(), List.of());
            }
            if (command.expectedVersion() != current.version()) conflict("Claim version is stale");
            Instant now = now();
            Decision decision = engine.evaluate(current, actor, command, now);
            var nextFacts = new LinkedHashMap<>(current.facts());
            if ("REJECTED".equals(decision.toState())) nextFacts.put("appealDeadline", now.plus(7, ChronoUnit.DAYS).toString());
            boolean paymentEffect = decision.effects().contains("create_payment_request");
            if (paymentEffect) {
                effects.createPaymentRequest(id, UUID.randomUUID());
                nextFacts.put("paymentRequestCreated", true);
            }
            ClaimSnapshot transitioned = current.transition(decision);
            var finalFacts = new LinkedHashMap<>(transitioned.facts());
            // Transition clearing rules take precedence; only server-generated facts are added here.
            if (paymentEffect) finalFacts.put("paymentRequestCreated", true);
            if ("REJECTED".equals(decision.toState())) finalFacts.put("appealDeadline", now.plus(7, ChronoUnit.DAYS).toString());
            ClaimSnapshot next = new ClaimSnapshot(transitioned.id(), transitioned.memberId(), transitioned.state(),
                    transitioned.policyLimit(), transitioned.currency(), finalFacts, transitioned.informationRequests(),
                    transitioned.version(), transitioned.assessmentStartedAt(), transitioned.lastInformationRequestedAt());
            if (!claims.compareAndSet(next, current.version())) conflict("Claim version is stale");
            String trace = Optional.ofNullable(MDC.get("traceId")).orElse("unavailable");
            audits.append(new AuditEntry(UUID.randomUUID(), id, next.version(), now, current.state(), next.state(),
                    actor.userId(), actor.role(), command.notes(), engine.configVersion(), trace));
            List<OutboxMessage> messages = new ArrayList<>();
            for (String effect : decision.effects()) {
                if (!effectFactory.supports(effect)) throw new IllegalStateException("Unsupported configured effect: " + effect);
                if (!"create_payment_request".equals(effect))
                    messages.add(effectFactory.create(effect, next, actor, command.notes()));
            }
            effects.enqueue(messages);
            CommandReceipt receipt = new CommandReceipt(hash, next);
            commands.insert(id, actor.userId(), command.idempotencyKey(), receipt);
            return new AtomicResult(next, messages);
        });
        // TransactionTemplate.execute has returned: console delivery cannot join or roll back business work.
        if (outcome != null && !outcome.messages().isEmpty()) dispatcher.dispatch(outcome.messages());
        return Objects.requireNonNull(outcome).claim();
    }

    @Override public List<TransitionOption> options(Actor actor, UUID id) {
        ClaimSnapshot claim = get(actor, id);
        if ("member".equals(actor.role())) return List.of();
        return engine.options(claim, actor, now());
    }
    @Override public List<AuditEntry> audit(Actor actor, UUID id) {
        ClaimSnapshot claim = get(actor, id); return audits.findByClaimId(claim.id());
    }

    private Map<String,Object> validateUpdates(Actor actor, ClaimSnapshot claim, Map<String,Object> updates) {
        Set<String> allowed = switch (actor.role()) {
            case "document_clerk" -> Set.of("documents", "newInformation");
            case "team_lead" -> Set.of("assessorId");
            case "assessor" -> Set.of("reportComplete", "assessmentAmount", "assessmentCurrency", "rejectionReason", "missingInfoDescription");
            case "finance" -> Set.of("paymentConfirmed", "paymentReference");
            case "member" -> Set.of("memberAcknowledged");
            default -> Set.of();
        };
        if (allowed.isEmpty()) forbidden();
        boolean phaseAllowed = switch (actor.role()) {
            case "document_clerk" -> Set.of("SUBMITTED", "PENDING_INFO").contains(claim.state());
            case "team_lead" -> "DOCUMENTS_VERIFIED".equals(claim.state());
            case "assessor" -> "UNDER_ASSESSMENT".equals(claim.state());
            case "finance" -> "PAYMENT_INITIATED".equals(claim.state());
            case "member" -> "REJECTED".equals(claim.state());
            default -> false;
        };
        if (!phaseAllowed) conflict("Facts cannot be changed in the current claim state");
        for (String key : updates.keySet()) if (!allowed.contains(key)) invalid("Fact is not writable by this role: " + key);
        if (updates.size() > 12) invalid("Too many fact updates");
        var accepted = new LinkedHashMap<String,Object>();
        for (var entry : updates.entrySet()) {
            String key = entry.getKey(); Object value = entry.getValue();
            switch (key) {
                case "documents" -> accepted.put(key, validateDocuments(claim, value));
                case "newInformation" -> {
                    String text = boundedText(value, 2000, key); accepted.put(key, text);
                    accepted.put("informationReceivedAt", now().toString());
                }
                case "assessorId" -> accepted.put(key, boundedText(value, 120, key));
                case "reportComplete", "memberAcknowledged", "paymentConfirmed" -> {
                    if (!(value instanceof Boolean)) invalid(key + " must be a boolean"); accepted.put(key, value);
                }
                case "assessmentAmount" -> accepted.put(key, validateAmount(value));
                case "assessmentCurrency" -> { if (!(value instanceof String s)) invalid(key + " must be text"); validateCurrency((String)value); accepted.put(key, value); }
                case "rejectionReason", "missingInfoDescription" -> accepted.put(key, boundedText(value, 2000, key));
                case "paymentReference" -> accepted.put(key, boundedText(value, 160, key));
                default -> invalid("Unsupported fact");
            }
        }
        if ("finance".equals(actor.role()) && (updates.containsKey("paymentConfirmed") || updates.containsKey("paymentReference"))) {
            if (!effects.paymentRequestExists(claim.id())) conflict("A payment request must exist before confirmation");
            Object reference = updates.getOrDefault("paymentReference", claim.facts().get("paymentReference"));
            if (!(reference instanceof String text) || text.isBlank()) invalid("Payment reference is required for confirmation");
        }
        return accepted;
    }
    private Map<String,Object> validateDocuments(ClaimSnapshot claim, Object value) {
        if (!(value instanceof Map<?,?> supplied) || supplied.size() > 30) invalid("documents must be a map");
        Object requiredValue = claim.facts().get("requiredDocuments");
        Set<String> required = requiredValue instanceof List<?> list ? new HashSet<>(list.stream().map(String::valueOf).toList()) : Set.of();
        var docs = new LinkedHashMap<String,Object>();
        for (var e : ((Map<?,?>)value).entrySet()) {
            if (!(e.getKey() instanceof String key) || !required.contains(key) || !(e.getValue() instanceof Boolean)) invalid("Document results must match required keys and be booleans");
            docs.put((String)e.getKey(), e.getValue());
        }
        return Collections.unmodifiableMap(docs);
    }
    private static BigDecimal validateAmount(Object value) {
        BigDecimal amount;
        try { amount = value instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(value)); }
        catch (RuntimeException error) { throw new BusinessException("INVALID_AMOUNT", 400, "Assessment amount must be a decimal number"); }
        if (amount.signum() < 0 || amount.scale() > 2 || amount.precision() > 18) invalid("Assessment amount must be nonnegative with at most two decimals");
        return amount;
    }
    private static String boundedText(Object value, int max, String field) {
        if (!(value instanceof String text) || text.isBlank() || text.length() > max) invalid(field + " must be nonblank text of at most " + max + " characters"); return text;
    }
    private static void validateCurrency(String currency) {
        try { if (currency == null || !currency.equals(currency.toUpperCase(Locale.ROOT)) || currency.length() != 3) throw new IllegalArgumentException(); Currency.getInstance(currency); }
        catch (IllegalArgumentException error) { throw new BusinessException("INVALID_CURRENCY", 400, "A valid uppercase ISO currency is required"); }
    }
    private ClaimSnapshot locked(UUID id) { return claims.findForUpdate(Objects.requireNonNull(id)).orElseThrow(ClaimService::notFound); }
    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private static String hash(TransitionCommand command) {
        try {
            String stable = command.target() + "\u0000" + command.expectedVersion() + "\u0000" + command.notes();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(stable.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private boolean canRead(Actor actor, ClaimSnapshot claim) { return "member".equals(actor.role()) ? actor.userId().equals(claim.memberId()) : STAFF.contains(actor.role()) || "system".equals(actor.role()); }
    private void requireReadable(Actor actor) { if (actor == null || !(STAFF.contains(actor.role()) || "member".equals(actor.role()) || "system".equals(actor.role()))) forbidden(); }
    private void requireRead(Actor actor, ClaimSnapshot claim) { requireReadable(actor); if (!canRead(actor, claim)) forbidden(); }
    private static void requireAny(Actor actor, Set<String> roles) { if (!roles.contains(actor.role())) forbidden(); }
    private static boolean terminal(String state) { return Set.of("CLOSED").contains(state); }
    private static void invalid(String message) { throw new BusinessException("INVALID_INPUT", 400, message); }
    private static void forbidden() { throw new BusinessException("FORBIDDEN_ROLE", 403, "The authenticated actor cannot perform this operation"); }
    private static void conflict(String message) { throw new BusinessException("CONFLICT", 409, message); }
    private static BusinessException notFound() { return new BusinessException("NOT_FOUND", 404, "Claim was not found"); }
    private record AtomicResult(ClaimSnapshot claim, List<OutboxMessage> messages) {}
}
