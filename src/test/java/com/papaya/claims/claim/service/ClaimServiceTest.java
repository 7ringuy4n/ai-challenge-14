package com.papaya.claims.claim.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.claim.domain.*;
import com.papaya.claims.workflow.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-17T02:00:00.123456Z");
    private static final UUID CLAIM_ID = UUID.fromString("d2a96309-5b93-4e4b-a127-c99320883dc9");

    @Test
    void businessExceptionRequiresSafeCodeMessageAndClientErrorStatus() {
        assertThatThrownBy(() -> new BusinessException(null, "message", 400)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BusinessException(" ", "message", 400)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BusinessException("CODE", null, 400)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BusinessException("CODE", " ", 400)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BusinessException("CODE", "message", 399)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BusinessException("CODE", "message", 600)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new BusinessException("CODE", "message", 400).status()).isEqualTo(400);
        assertThat(new BusinessException("CODE", "message", 599).status()).isEqualTo(599);
    }

    @Test
    void ownerAcknowledgementPersistsMicrosecondServerTimeAndIdempotencyReceipt() {
        Fixture fixture = new Fixture("REJECTED", "member-1", Map.of(),
                new ClaimWorkflowData(0, null, null, null, NOW.minusSeconds(30), null));

        Claim updated = fixture.service.acknowledgeRejection(CLAIM_ID, 4,
                new Actor("member-1", Set.of("member"), false), "ack-1", "corr-1");

        assertThat(updated.version()).isEqualTo(5);
        assertThat(updated.updatedAt()).isEqualTo(NOW.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        assertThat(fixture.claims.current.workflow().memberAcknowledgedRejectionAt())
                .isEqualTo(NOW.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        assertThat(fixture.receipts.receipt).isNotNull();
        assertThat(fixture.systemAudit.records).hasSize(1);
    }

    @Test
    void acknowledgementRequiresMemberRoleAndReplaysMatchingRequestWithoutAnotherWrite() {
        Fixture fixture = new Fixture("REJECTED", "member-1", Map.of(),
                new ClaimWorkflowData(0, null, null, null, NOW.minusSeconds(30), null));
        assertThatThrownBy(() -> fixture.service.acknowledgeRejection(CLAIM_ID, 4,
                new Actor("assessor", Set.of("assessor"), false), "ack-role", "corr"))
                .isInstanceOf(BusinessException.class).hasMessage("Required role is missing");
        Actor member = new Actor("member-1", Set.of("member"), false);
        Claim first = fixture.service.acknowledgeRejection(CLAIM_ID, 4, member, "ack-replay", "corr");
        Claim replay = fixture.service.acknowledgeRejection(CLAIM_ID, 4, member, "ack-replay", "corr");
        assertThat(replay.version()).isEqualTo(first.version());
        assertThat(fixture.claims.writes).isEqualTo(1);
        assertThat(fixture.systemAudit.records).hasSize(1);
    }

    @Test
    void memberCannotAcknowledgeAnotherMembersClaim() {
        Fixture fixture = new Fixture("REJECTED", "member-1", Map.of(),
                new ClaimWorkflowData(0, null, null, null, NOW.minusSeconds(30), null));
        assertThatThrownBy(() -> fixture.service.acknowledgeRejection(CLAIM_ID, 4,
                new Actor("member-2", Set.of("member"), false), "ack-2", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessage("Claim was not found");
        assertThat(fixture.claims.writes).isZero();
    }

    @Test
    void acknowledgementRequiresRejectedStateAndExpectedVersion() {
        Fixture fixture = new Fixture("UNDER_ASSESSMENT", "member-1", Map.of(), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> fixture.service.acknowledgeRejection(CLAIM_ID, 4,
                new Actor("member-1", Set.of("member"), false), "ack-3", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessage("Only a rejected claim can be acknowledged");
        assertThat(fixture.claims.writes).isZero();
    }

    @Test
    void assessorMustMatchTheAssignmentToWriteAnAssessmentReport() {
        Fixture fixture = new Fixture("UNDER_ASSESSMENT", "member-1", Map.of("assessorId", "assessor-1"), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> fixture.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("assessmentReport", Map.of("complete", true)))),
                new Actor("assessor-2", Set.of("assessor"), false), "facts-1", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessage("Only the assigned assessor may submit an assessment report");
        assertThat(fixture.claims.writes).isZero();
    }

    @Test
    void optionsAreDerivedFromConfiguredEdgesAndCurrentRole() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        List<WorkflowOption> options = fixture.service.options(CLAIM_ID,
                new Actor("clerk-1", Set.of("document_clerk"), false));
        assertThat(options).hasSize(1);
        assertThat(options.getFirst().transitionId()).isEqualTo("verify-documents");
        assertThat(options.getFirst().authorizedRoles()).containsExactly("document_clerk");
        assertThat(options.getFirst().available()).isFalse();
    }

    @Test
    void createPersistsClaimAuditAndReceiptAndReplaysSameRequest() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        var command = new CreateClaimCommand(new BigDecimal("500.00"), "USD",
                new ClaimFacts(Map.of("requestedAmount", new BigDecimal("125.50"))));
        Actor member = new Actor("member-1", Set.of("member"), false);

        Claim created = fixture.service.create(command, member, "create-1", "corr-1");
        Claim replay = fixture.service.create(command, member, "create-1", "corr-1");

        assertThat(created.id()).isEqualTo(replay.id());
        assertThat(fixture.claims.inserts).isEqualTo(1);
        assertThat(fixture.systemAudit.records).hasSize(1);
        assertThat(fixture.receipts.receipt).isNotNull();
    }

    @Test
    void idempotencyFingerprintCanonicalizesObjectKeyOrderAndRejectsCorruptStoredResult() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1",
                Map.of("documents", Map.of("invoice", Map.of("required", true, "present", false, "valid", false))), ClaimWorkflowData.empty());
        Actor clerk = new Actor("clerk", Set.of("document_clerk"), false);
        Map<String, Object> firstDocument = new java.util.LinkedHashMap<>();
        firstDocument.put("present", true);
        firstDocument.put("valid", true);
        Claim first = fixture.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("documents", Map.of("invoice", firstDocument)))),
                clerk, "canonical", "corr");
        Map<String, Object> reorderedDocument = new java.util.LinkedHashMap<>();
        reorderedDocument.put("valid", true);
        reorderedDocument.put("present", true);
        Claim replay = fixture.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("documents", Map.of("invoice", reorderedDocument)))),
                clerk, "canonical", "corr");
        assertThat(replay.version()).isEqualTo(first.version());

        Fixture corrupt = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        Actor member = new Actor("member-1", Set.of("member"), false);
        CreateClaimCommand create = new CreateClaimCommand(new BigDecimal("50.00"), "USD",
                new ClaimFacts(Map.of("requestedAmount", new BigDecimal("25.00"))));
        corrupt.service.create(create, member, "corrupt", "corr");
        IdempotencyRepository.Receipt stored = corrupt.receipts.receipt;
        corrupt.receipts.receipt = new IdempotencyRepository.Receipt(stored.id(), stored.actorId(), stored.scope(),
                stored.idempotencyKey(), stored.fingerprint(), "not-json");
        assertThatThrownBy(() -> corrupt.service.create(create, member, "corrupt", "corr"))
                .isInstanceOf(IllegalStateException.class).hasMessage("Stored idempotency result is invalid");
    }

    @Test
    void createRejectsReusedKeyWithDifferentPayloadBeforeWriting() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        Actor member = new Actor("member-1", Set.of("member"), false);
        fixture.service.create(new CreateClaimCommand(new BigDecimal("500.00"), "USD",
                new ClaimFacts(Map.of("requestedAmount", new BigDecimal("125.50")))), member, "create-2", "corr-1");

        assertThatThrownBy(() -> fixture.service.create(new CreateClaimCommand(new BigDecimal("500.00"), "USD",
                new ClaimFacts(Map.of("requestedAmount", new BigDecimal("126.50")))), member, "create-2", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already used");
        assertThat(fixture.claims.inserts).isEqualTo(1);
    }

    @Test
    void createValidatesActorKeyAndExactRequestedAmount() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        var valid = new CreateClaimCommand(new BigDecimal("500.00"), "USD",
                new ClaimFacts(Map.of("requestedAmount", new BigDecimal("125.50"))));
        assertThatThrownBy(() -> fixture.service.create(valid, new Actor("service", Set.of("system"), true), "k", "c"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("System identity cannot");
        assertThatThrownBy(() -> fixture.service.create(valid, new Actor("member-1", Set.of("member"), false), " ", "c"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Idempotency-Key");
        assertThatThrownBy(() -> fixture.service.create(valid, null, "missing-actor", "c"))
                .isInstanceOf(BusinessException.class).hasMessage("Authentication is required");
        var tooPrecise = new CreateClaimCommand(new BigDecimal("500.00"), "USD",
                new ClaimFacts(Map.of("requestedAmount", new BigDecimal("1.001"))));
        assertThatThrownBy(() -> fixture.service.create(tooPrecise, new Actor("member-1", Set.of("member"), false), "k", "c"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("exact decimal");
        assertThat(fixture.claims.inserts).isZero();
    }

    @Test
    void createRejectsNegativeAndNonDecimalAmounts() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        Actor member = new Actor("member-1", Set.of("member"), false);
        assertThatThrownBy(() -> fixture.service.create(new CreateClaimCommand(new BigDecimal("10.00"), "USD",
                new ClaimFacts(Map.of("requestedAmount", new BigDecimal("-0.01")))), member, "negative", "corr"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("nonnegative exact decimal");
        assertThatThrownBy(() -> fixture.service.create(new CreateClaimCommand(new BigDecimal("10.00"), "USD",
                new ClaimFacts(Map.of("requestedAmount", "1.00"))), member, "string-amount", "corr"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("exact decimal");
        assertThat(fixture.claims.inserts).isZero();
    }

    @Test
    void createRejectsMissingOutOfRangeAndExtraFactsWithoutPersisting() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        Actor member = new Actor("member-1", Set.of("member"), false);
        assertThatThrownBy(() -> fixture.service.create(new CreateClaimCommand(new BigDecimal("10.00"), "USD",
                new ClaimFacts(Map.of())), member, "missing-amount", "corr"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("requestedAmount must be present");
        assertThatThrownBy(() -> fixture.service.create(new CreateClaimCommand(new BigDecimal("10.00"), "USD",
                new ClaimFacts(Map.of("requestedAmount", new BigDecimal("10.01")))), member, "over-limit", "corr"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("no greater than the policy limit");
        assertThatThrownBy(() -> fixture.service.create(new CreateClaimCommand(new BigDecimal("10.00"), "USD",
                new ClaimFacts(Map.of("requestedAmount", BigDecimal.ONE, "assessorId", "assessor-1"))), member,
                "extra-fact", "corr"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Only requestedAmount");
        assertThat(fixture.claims.inserts).isZero();
    }

    @Test
    void updateRejectsUnauthenticatedUnrecognizedActorAndStaleExpectedVersion() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        UpdateFactsCommand command = new UpdateFactsCommand(4, new ClaimFacts(Map.of("assessorId", "a-1")));
        assertThatThrownBy(() -> fixture.service.updateFacts(CLAIM_ID, command, null, "u-1", "corr"))
                .isInstanceOf(BusinessException.class).hasMessage("Authentication is required");
        assertThatThrownBy(() -> fixture.service.updateFacts(CLAIM_ID, command,
                new Actor("guest", Set.of(), false), "u-2", "corr"))
                .isInstanceOf(BusinessException.class).hasMessage("Authenticated human role is required");
        assertThatThrownBy(() -> fixture.service.updateFacts(CLAIM_ID, new UpdateFactsCommand(3, command.facts()),
                new Actor("lead", Set.of("team_lead"), false), "u-3", "corr"))
                .isInstanceOf(BusinessException.class).hasMessage("Claim version is stale");
        assertThat(fixture.claims.writes).isZero();
    }

    @Test
    void systemTransitionsRequireRoleAndConfiguredSystemEdgeAndDoNotExposeOtherMemberClaims() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> fixture.service.systemTransition(CLAIM_ID,
                new TransitionCommand("CLOSED", 4, "", new ClaimFacts(Map.of())), null, "s-0", "corr"))
                .isInstanceOf(BusinessException.class).hasMessage("Authentication is required");
        assertThatThrownBy(() -> fixture.service.systemTransition(CLAIM_ID,
                new TransitionCommand("CLOSED", 4, "", new ClaimFacts(Map.of())),
                new Actor("worker", Set.of("assessor"), false), "s-1", "corr"))
                .isInstanceOf(BusinessException.class).hasMessage("Protected system identity is required");
        assertThatThrownBy(() -> fixture.service.get(CLAIM_ID, new Actor("member-2", Set.of("member"), false)))
                .isInstanceOf(BusinessException.class).hasMessage("Claim was not found");
    }

    @Test
    void updateFactsEnforcesDocumentOwnershipAndRejectsMalformedOrUnauthorizedPatches() {
        Actor otherMember = new Actor("member-2", Set.of("claims_admin"), false);
        Fixture owner = new Fixture("PENDING_INFO", "member-1",
                Map.of("documents", Map.of("invoice", Map.of("required", true, "present", false, "valid", false))), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> owner.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("documents", Map.of("invoice", Map.of("present", true))))),
                otherMember, "foreign-doc", "corr"))
                .isInstanceOf(BusinessException.class).hasMessage("Only the claim member or document clerk may update documents");
        assertDocumentUpdateFails("SUBMITTED", Map.of(), new Actor("clerk", Set.of("document_clerk"), false),
                "documents must be a nonempty object");
        assertDocumentUpdateFails("SUBMITTED", Map.of("invoice", Map.of("present", "yes")),
                new Actor("clerk", Set.of("document_clerk"), false), "Document fields must be booleans");
        assertDocumentUpdateFails("SUBMITTED", Map.of("invoice", Map.of("required", true, "present", false, "valid", false)),
                new Actor("outsider", Set.of("claims_admin"), false), "Only the claim member or document clerk");
    }

    @Test
    void factsRequireNonemptyAllowedFieldsAndCorrectRole() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        Actor member = new Actor("member-1", Set.of("member"), false);
        assertThatThrownBy(() -> fixture.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of())), member, "fact-1", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("At least one fact");
        assertThatThrownBy(() -> fixture.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("private", true))), member, "fact-2", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("not writable");
        assertThat(fixture.claims.writes).isZero();
    }

    @Test
    void memberCanOnlyUpdatePresenceOnExistingRosterWhilePendingInformation() {
        Map<String, Object> documents = Map.of("invoice", Map.of("required", true, "present", false, "valid", false));
        Fixture fixture = new Fixture("PENDING_INFO", "member-1", Map.of("documents", documents), ClaimWorkflowData.empty());
        Actor member = new Actor("member-1", Set.of("member"), false);
        Claim updated = fixture.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("documents", Map.of("invoice", Map.of("present", true))))),
                member, "doc-1", "corr-1");
        assertThat(((Map<?, ?>)((Map<?, ?>)updated.facts().values().get("documents")).get("invoice")).get("present")).isEqualTo(true);

        Fixture invalid = new Fixture("PENDING_INFO", "member-1", Map.of("documents", documents), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> invalid.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("documents", Map.of("new-doc", Map.of("present", true))))),
                member, "doc-2", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("cannot add document roster");
        assertThat(invalid.claims.writes).isZero();
    }

    @Test
    void absentClaimsAreNotFoundAndNullActorIsForbidden() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> fixture.service.get(UUID.randomUUID(), new Actor("staff", Set.of("claims_admin"), false)))
                .isInstanceOf(BusinessException.class).hasMessage("Claim was not found");
        assertThatThrownBy(() -> fixture.service.listVisible(new PageRequest(0, 10), null))
                .isInstanceOf(BusinessException.class).hasMessage("Authentication is required");
    }

    @Test
    void updatesReplayByFingerprintAndRejectStaleVersion() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        Actor teamLead = new Actor("lead-1", Set.of("team_lead"), false);
        var command = new UpdateFactsCommand(4, new ClaimFacts(Map.of("assessorId", "assessor-1")));
        Claim first = fixture.service.updateFacts(CLAIM_ID, command, teamLead, "facts-replay", "corr-1");
        Claim replay = fixture.service.updateFacts(CLAIM_ID, command, teamLead, "facts-replay", "corr-1");
        assertThat(first.version()).isEqualTo(5);
        assertThat(replay.version()).isEqualTo(5);
        assertThat(fixture.claims.writes).isEqualTo(1);
        assertThat(fixture.systemAudit.records).hasSize(1);

        Fixture stale = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        stale.claims.current = new ClaimAggregate(new Claim(CLAIM_ID, "member-1", "SUBMITTED", new BigDecimal("100.00"),
                "USD", 5, new ClaimFacts(Map.of()), NOW.minusSeconds(60), NOW), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> stale.service.updateFacts(CLAIM_ID, command, teamLead, "facts-stale", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessage("Claim version is stale");
    }

    @Test
    void transitionRequiresHumanIdentityAndConfiguredAllowedEdge() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        var command = new TransitionCommand("DOCUMENTS_VERIFIED", 4, "verified", new ClaimFacts(Map.of()));
        Actor clerk = new Actor("clerk-1", Set.of("document_clerk"), false);
        assertThatThrownBy(() -> fixture.service.transition(CLAIM_ID, command, new Actor("robot", Set.of("system"), true), "t-1", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("System identity cannot");
        assertThatThrownBy(() -> fixture.service.transition(CLAIM_ID,
                new TransitionCommand("CLOSED", 4, "", new ClaimFacts(Map.of())), clerk, "t-2", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("No configured transition");
        assertThatThrownBy(() -> fixture.service.transition(CLAIM_ID, command,
                new Actor("lead-1", Set.of("team_lead"), false), "t-3", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessage("Actor is not authorized for this transition");
        assertThat(fixture.claims.writes).isZero();
    }

    @Test
    void transitionReplaysWithReplayedMarkerAndRequiresSafeIdempotencyKey() {
        Map<String, Object> facts = Map.of("documents", Map.of("invoice", Map.of("required", true, "present", true, "valid", true)));
        Fixture fixture = new Fixture("SUBMITTED", "member-1", facts, ClaimWorkflowData.empty());
        Actor clerk = new Actor("clerk-1", Set.of("document_clerk"), false);
        var command = new TransitionCommand("DOCUMENTS_VERIFIED", 4, "verified", new ClaimFacts(Map.of()));
        TransitionResult first = fixture.service.transition(CLAIM_ID, command, clerk, "transition-1", "corr-1");
        TransitionResult replay = fixture.service.transition(CLAIM_ID, command, clerk, "transition-1", "corr-1");
        assertThat(first.replayed()).isFalse();
        assertThat(replay.replayed()).isTrue();
        assertThat(fixture.claims.writes).isEqualTo(1);
        assertThat(fixture.systemAudit.records).hasSize(1);
        assertThat(fixture.claimAudits.records).hasSize(1);

        assertThatThrownBy(() -> fixture.service.transition(CLAIM_ID, command, clerk, "bad key", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Idempotency-Key");
    }

    @Test
    void systemTransitionRequiresProtectedSystemActorAndAuditReadsRequireVisibility() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        var command = new TransitionCommand("DOCUMENTS_VERIFIED", 4, "verified", new ClaimFacts(Map.of()));
        assertThatThrownBy(() -> fixture.service.systemTransition(CLAIM_ID, command,
                new Actor("ordinary", Set.of("system"), false), "sys-1", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessage("Protected system identity is required");
        assertThatThrownBy(() -> fixture.service.audit(CLAIM_ID, new PageRequest(0, 10),
                new Actor("other-member", Set.of("member"), false)))
                .isInstanceOf(BusinessException.class).hasMessage("Claim was not found");
        assertThat(fixture.claimAudits.records).isEmpty();
        assertThatThrownBy(() -> fixture.service.systemTransition(CLAIM_ID,
                new TransitionCommand("CLOSED", 4, "", new ClaimFacts(Map.of())),
                new Actor("system", Set.of("system"), true), "sys-2", "corr"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("No configured transition");
    }

    @Test
    void documentClerkCanCreateAndUpdateCompleteRosterEntries() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        Actor clerk = new Actor("clerk-1", Set.of("document_clerk"), false);
        Claim created = fixture.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("documents",
                        Map.of("invoice-1", Map.of("required", true, "present", false, "valid", false))))),
                clerk, "clerk-doc-1", "corr-1");
        assertThat(documentsOf(created).get("invoice-1")).containsEntry("required", true);

        Claim updated = fixture.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(5, new ClaimFacts(Map.of("documents",
                        Map.of("invoice-1", Map.of("present", true, "valid", true))))),
                clerk, "clerk-doc-2", "corr-1");
        assertThat(documentsOf(updated).get("invoice-1")).containsEntry("present", true).containsEntry("valid", true);
    }

    @Test
    void requiredDocumentFlagCanChangeOnlyBeforeSubmissionAndCorrelationIdsAreSanitized() {
        Map<String, Object> initial = Map.of("documents", Map.of("invoice", Map.of("required", true, "present", false, "valid", false)));
        Fixture submitted = new Fixture("SUBMITTED", "member-1", initial, ClaimWorkflowData.empty());
        Claim updated = submitted.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("documents", Map.of("invoice", Map.of("required", false))))),
                new Actor("clerk", Set.of("document_clerk"), false), "required-before-submit", "unsafe correlation");
        assertThat(documentsOf(updated).get("invoice")).containsEntry("required", false);
        assertThat(submitted.systemAudit.records).singleElement().satisfies(record ->
                assertThat(record.correlationId()).matches("[0-9a-f-]{36}"));
    }

    @Test
    void documentValidationRejectsMalformedRosterAndUnknownFields() {
        Actor clerk = new Actor("clerk-1", Set.of("document_clerk"), false);
        assertDocumentUpdateFails("SUBMITTED", Map.of("bad id", Map.of("required", true, "present", true, "valid", true)), clerk,
                "Document ID is invalid");
        assertDocumentUpdateFails("SUBMITTED", Map.of("invoice", "not-an-object"), clerk,
                "Document fields must be a nonempty object");
        assertDocumentUpdateFails("SUBMITTED", Map.of("invoice", Map.of("required", true)), clerk,
                "Document entry must contain");
        assertDocumentUpdateFails("SUBMITTED", Map.of("invoice", Map.of("required", true, "present", true, "valid", true, "unexpected", false)), clerk,
                "Unknown document field");
    }

    @Test
    void documentRosterRulesEnforceStateAndImmutableRequiredFlag() {
        Actor member = new Actor("member-1", Set.of("member"), false);
        assertDocumentUpdateFails("SUBMITTED", Map.of("invoice", Map.of("present", true)), member,
                "pending information");
        Map<String, Object> roster = Map.of("invoice", Map.of("required", true, "present", false, "valid", false));
        Fixture pending = new Fixture("PENDING_INFO", "member-1", Map.of("documents", roster), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> pending.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("documents", Map.of("invoice", Map.of("valid", true))))),
                member, "member-valid", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("only update document presence");

        Fixture verified = new Fixture("DOCUMENTS_VERIFIED", "member-1", Map.of("documents", roster), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> verified.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("documents", Map.of("invoice", Map.of("required", false))))),
                new Actor("clerk-2", Set.of("document_clerk"), false), "clerk-required", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Documents cannot be updated");

        Fixture pendingClerk = new Fixture("PENDING_INFO", "member-1", Map.of("documents", roster), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> pendingClerk.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("documents", Map.of("invoice", Map.of("required", false))))),
                new Actor("clerk-3", Set.of("document_clerk"), false), "clerk-required-2", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("required status is immutable");
    }

    @Test
    void assessorAssignmentAndReportValidationAreStateAndRoleBound() {
        Actor lead = new Actor("lead-1", Set.of("team_lead"), false);
        Fixture assessmentStarted = new Fixture("UNDER_ASSESSMENT", "member-1", Map.of(), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> assessmentStarted.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("assessorId", "a-1"))), lead, "assign-late", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Only a team lead may assign");
        Fixture blankAssessor = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> blankAssessor.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("assessorId", "  "))), lead, "assign-blank", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessage("assessorId must be a nonblank string");

        Fixture malformedReport = new Fixture("UNDER_ASSESSMENT", "member-1", Map.of("assessorId", "assessor-1"), ClaimWorkflowData.empty());
        Actor assessor = new Actor("assessor-1", Set.of("assessor"), false);
        assertThatThrownBy(() -> malformedReport.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("assessmentReport", Map.of("complete", "yes")))), assessor, "report-bad", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessage("assessmentReport must contain a boolean complete field");
    }

    @Test
    void compareAndSetRacesDoNotWriteAuditsOrReceipts() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        fixture.claims.allowCas = false;
        assertThatThrownBy(() -> fixture.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("assessorId", "assessor-1"))),
                new Actor("lead", Set.of("team_lead"), false), "cas-fail", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessage("Claim version is stale");
        assertThat(fixture.systemAudit.records).isEmpty();
        assertThat(fixture.receipts.receipt).isNull();
    }

    @Test
    void auditAndListPreserveVisibilityAndPageMetadata() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        var page = new PageRequest(1, 5);
        PageResult<Claim> visible = fixture.service.listVisible(page, new Actor("staff", Set.of("claims_admin"), false));
        assertThat(visible.request()).isEqualTo(page);
        assertThat(visible.items()).extracting(Claim::id).containsExactly(CLAIM_ID);
        assertThat(fixture.service.audit(CLAIM_ID, new PageRequest(0, 5), new Actor("member-1", Set.of("member"), false)).total()).isZero();
    }

    @Test
    void approvalTransitionPersistsPaymentAndBothOutboxEffectsAtomicallyAtTheServiceBoundary() {
        Fixture fixture = new Fixture("UNDER_ASSESSMENT", "member-1",
                Map.of("requestedAmount", new BigDecimal("75.25")), ClaimWorkflowData.empty());
        TransitionResult result = fixture.service.transition(CLAIM_ID,
                new TransitionCommand("APPROVED", 4, "approved", new ClaimFacts(Map.of())),
                new Actor("assessor-1", Set.of("assessor"), false), "approve-1", "corr-1");
        assertThat(result.claim().state()).isEqualTo("APPROVED");
        assertThat(fixture.payments.creates).singleElement().satisfies(created -> {
            assertThat(created.amount()).isEqualByComparingTo("75.25");
            assertThat(created.currency()).isEqualTo("USD");
        });
        assertThat(fixture.outbox.messages).extracting(OutboxRepository.Message::effect)
                .containsExactly("notify_member_approval", "create_payment_request");
        assertThat(fixture.outbox.messages).allSatisfy(message -> assertThat(message.availableAt()).isEqualTo(NOW.plusSeconds(1)));
    }

    @Test
    void transitionRejectsStaleAndForbiddenWithoutWritingAndReplaysOnlyMatchingFingerprint() {
        Fixture fixture = new Fixture("SUBMITTED", "member-1",
                Map.of("documents", Map.of("invoice", Map.of("required", true, "present", true, "valid", true))), ClaimWorkflowData.empty());
        Actor clerk = new Actor("clerk-1", Set.of("document_clerk"), false);
        TransitionCommand command = new TransitionCommand("DOCUMENTS_VERIFIED", 4, "verified", new ClaimFacts(Map.of()));
        fixture.service.transition(CLAIM_ID, command, clerk, "same-key", "corr");
        assertThatThrownBy(() -> fixture.service.transition(CLAIM_ID,
                new TransitionCommand("DOCUMENTS_VERIFIED", 4, "different", new ClaimFacts(Map.of())), clerk, "same-key", "corr"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already used");

        Fixture stale = new Fixture("SUBMITTED", "member-1", Map.of(), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> stale.service.transition(CLAIM_ID,
                new TransitionCommand("DOCUMENTS_VERIFIED", 3, "verified", new ClaimFacts(Map.of())), clerk, "stale", "corr"))
                .isInstanceOf(BusinessException.class).hasMessage("Claim version is stale");
        assertThat(stale.claims.writes).isZero();
    }

    @Test
    void workflowEffectsUpdateTrustedTimestampsCountersAndClearStaleAssessmentFacts() {
        Map<String, Object> report = Map.of("assessmentReport", Map.of("complete", true));
        Fixture fixture = new Fixture("UNDER_ASSESSMENT", "member-1", report,
                new ClaimWorkflowData(0, null, null, NOW.minusSeconds(60), null, null));
        Actor assessor = new Actor("assessor-1", Set.of("assessor"), false);
        TransitionResult requested = fixture.service.transition(CLAIM_ID,
                new TransitionCommand("PENDING_INFO", 4, "please send receipt", new ClaimFacts(Map.of())),
                assessor, "info-request", "corr-1");
        assertThat(fixture.claims.current.workflow().informationRequests()).isEqualTo(1);
        assertThat(fixture.claims.current.workflow().lastInformationRequestedAt()).isEqualTo(NOW);
        assertThat(fixture.claims.current.workflow().assessmentStartedAt()).isEqualTo(NOW.minusSeconds(60));
        assertThat(requested.claim().facts().values()).containsKey("assessmentReport");

        fixture.claims.current = new ClaimAggregate(fixture.claims.current.claim(),
                new ClaimWorkflowData(1, NOW.minusSeconds(30), NOW, NOW.minusSeconds(60), null, null));
        TransitionResult returned = fixture.service.transition(CLAIM_ID,
                new TransitionCommand("DOCUMENTS_VERIFIED", 5, "documents supplied", new ClaimFacts(Map.of())),
                new Actor("clerk-1", Set.of("document_clerk"), false), "info-return", "corr-1");
        assertThat(returned.claim().facts().values()).doesNotContainKey("assessmentReport");
        assertThat(fixture.claims.current.workflow().assessmentStartedAt()).isNull();
        assertThat(fixture.outbox.messages).extracting(OutboxRepository.Message::effect)
                .containsExactly("notify_information_request", "reset_assessment_timer");
    }

    @Test
    void rejectionEffectStartsNewAppealWindowAndClearsPreviousAcknowledgement() {
        Fixture fixture = new Fixture("UNDER_ASSESSMENT", "member-1", Map.of("assessmentReport", Map.of("complete", true)),
                new ClaimWorkflowData(0, null, null, NOW.minusSeconds(20), NOW.minusSeconds(40), NOW.minusSeconds(30)));
        fixture.service.transition(CLAIM_ID,
                new TransitionCommand("REJECTED", 4, "not covered", new ClaimFacts(Map.of())),
                new Actor("assessor-1", Set.of("assessor"), false), "reject-1", "corr-1");
        assertThat(fixture.claims.current.workflow().rejectedAt()).isEqualTo(NOW);
        assertThat(fixture.claims.current.workflow().memberAcknowledgedRejectionAt()).isNull();
        assertThat(fixture.outbox.messages).singleElement().satisfies(message -> {
            assertThat(message.effect()).isEqualTo("notify_member_rejection");
            assertThat(message.payload()).contains("not covered");
        });
    }

    private void assertDocumentUpdateFails(String state, Map<String, Object> patch, Actor actor, String message) {
        Fixture fixture = new Fixture(state, "member-1", Map.of(), ClaimWorkflowData.empty());
        assertThatThrownBy(() -> fixture.service.updateFacts(CLAIM_ID,
                new UpdateFactsCommand(4, new ClaimFacts(Map.of("documents", patch))), actor, "invalid-doc", "corr-1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining(message);
        assertThat(fixture.claims.writes).isZero();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> documentsOf(Claim claim) {
        return (Map<String, Map<String, Object>>) claim.facts().values().get("documents");
    }

    private static final class Fixture {
        final MemoryClaimRepository claims;
        final MemoryReceiptRepository receipts = new MemoryReceiptRepository();
        final MemorySystemAuditRepository systemAudit = new MemorySystemAuditRepository();
        final MemoryClaimAuditRepository claimAudits = new MemoryClaimAuditRepository();
        final MemoryPaymentRepository payments = new MemoryPaymentRepository();
        final MemoryOutboxRepository outbox = new MemoryOutboxRepository();
        int inserts;
        final ClaimServiceImpl service;

        Fixture(String state, String memberId, Map<String, Object> facts, ClaimWorkflowData evidence) {
            Claim claim = new Claim(CLAIM_ID, memberId, state, new BigDecimal("100.00"), "USD", 4,
                    new ClaimFacts(facts), NOW.minusSeconds(60), NOW.minusSeconds(1));
            claims = new MemoryClaimRepository(new ClaimAggregate(claim, evidence));
            WorkflowDefinition definition = new WorkflowDefinition("test-v1", "SUBMITTED",
                    Set.of("SUBMITTED", "DOCUMENTS_VERIFIED", "UNDER_ASSESSMENT", "PENDING_INFO", "APPROVED", "REJECTED", "CLOSED"),
                    List.of(
                            new WorkflowDefinition.Transition("verify-documents", "SUBMITTED", "DOCUMENTS_VERIFIED",
                                    Set.of("document_clerk"), List.of("all_required_documents_present"), List.of("notify_assessor_team")),
                            new WorkflowDefinition.Transition("start-assessment", "DOCUMENTS_VERIFIED", "UNDER_ASSESSMENT",
                                    Set.of("team_lead"), List.of(), List.of("record_assessment_start")),
                            new WorkflowDefinition.Transition("approve-claim", "UNDER_ASSESSMENT", "APPROVED",
                                    Set.of("assessor"), List.of(), List.of("notify_member_approval", "create_payment_request")),
                            new WorkflowDefinition.Transition("reject-claim", "UNDER_ASSESSMENT", "REJECTED",
                                    Set.of("assessor"), List.of(), List.of("notify_member_rejection")),
                            new WorkflowDefinition.Transition("request-information", "UNDER_ASSESSMENT", "PENDING_INFO",
                                    Set.of("assessor"), List.of(), List.of("notify_information_request")),
                            new WorkflowDefinition.Transition("return-with-information", "PENDING_INFO", "DOCUMENTS_VERIFIED",
                                    Set.of("document_clerk"), List.of(), List.of("reset_assessment_timer"))),
                    Map.of("all_required_documents_present", "Documents are incomplete"), Map.of(),
                    new WorkflowDefinition.CyclePolicy("request-information", "informationRequests", 3, "cycle limit"));
            WorkflowEngine engine = new WorkflowEngine(definition, new GuardEvaluator(), new EffectRegistry());
            service = new ClaimServiceImpl(claims, claimAudits, systemAudit, receipts,
                    payments, outbox, engine, definition,
                    Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(30), new ObjectMapper().findAndRegisterModules(), 1000);
        }
    }

    private static final class MemoryClaimRepository implements ClaimRepository {
        ClaimAggregate current; int writes; int inserts; boolean allowCas = true;
        MemoryClaimRepository(ClaimAggregate current) { this.current = current; }
        public void insert(ClaimAggregate value) { current = value; inserts++; }
        public Optional<ClaimAggregate> find(UUID id) { return id.equals(current.claim().id()) ? Optional.of(current) : Optional.empty(); }
        public Optional<ClaimAggregate> findForUpdate(UUID id) { return find(id); }
        public PageResult<ClaimAggregate> listVisible(PageRequest page, Actor actor) { return new PageResult<>(List.of(current), 1, page); }
        public boolean compareAndSet(ClaimAggregate value, long expected) {
            if (!allowCas) return false;
            if (current.claim().version() != expected) return false;
            current = value; writes++; return true;
        }
    }
    private static final class MemoryReceiptRepository implements IdempotencyRepository {
        Receipt receipt;
        public void lock(String actor, String scope, String key) { }
        public Optional<Receipt> find(String actor, String scope, String key) {
            return receipt != null && receipt.actorId().equals(actor) && receipt.scope().equals(scope)
                    && receipt.idempotencyKey().equals(key) ? Optional.of(receipt) : Optional.empty();
        }
        public void insert(Receipt value) { receipt = value; }
    }
    private static final class MemorySystemAuditRepository implements SystemOperationAuditRepository {
        final java.util.ArrayList<SystemOperationAuditRecord> records = new java.util.ArrayList<>();
        public void append(SystemOperationAuditRecord value) { records.add(value); }
    }
    private static final class MemoryClaimAuditRepository implements ClaimAuditRepository {
        final java.util.ArrayList<AuditRecord> records = new java.util.ArrayList<>();
        public void append(AuditRecord record) { records.add(record); }
        public PageResult<AuditRecord> findByClaim(UUID id, PageRequest page) { return new PageResult<>(records, records.size(), page); }
    }
    private static final class MemoryPaymentRepository implements PaymentRepository {
        final java.util.ArrayList<CreatedPayment> creates = new java.util.ArrayList<>();
        public void create(UUID claimId, UUID requestId, String currency, BigDecimal amount) { creates.add(new CreatedPayment(claimId, requestId, currency, amount)); }
        public boolean exists(UUID claimId) { return creates.stream().anyMatch(created -> created.claimId().equals(claimId)); }
        public Optional<Confirmation> confirmation(UUID claimId) { return Optional.empty(); }
        public boolean confirm(UUID claimId, String reference) { return false; }
    }
    private record CreatedPayment(UUID claimId, UUID requestId, String currency, BigDecimal amount) { }
    private static final class MemoryOutboxRepository implements OutboxRepository {
        final java.util.ArrayList<Message> messages = new java.util.ArrayList<>();
        public void enqueue(Message message) { messages.add(message); }
        public List<Message> claimDue(Instant now, int limit) { return List.of(); }
        public boolean markDelivered(UUID id, Instant at) { return true; }
        public boolean markFailed(UUID id, String error, Instant retry) { return true; }
    }
}
