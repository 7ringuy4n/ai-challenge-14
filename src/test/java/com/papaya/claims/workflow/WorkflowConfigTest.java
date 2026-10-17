package com.papaya.claims.workflow;

import com.papaya.claims.claim.domain.Actor;
import com.papaya.claims.claim.domain.Claim;
import com.papaya.claims.claim.domain.ClaimFacts;
import com.papaya.claims.claim.domain.TransitionCommand;
import com.papaya.claims.config.WorkflowConfiguration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.junit.jupiter.api.Test;

class WorkflowConfigTest {
    private final WorkflowConfigLoader loader = new WorkflowConfigLoader();

    @Test
    void loadsFrozenWorkflowAndCyclePolicy() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/workflow.yml")) {
            WorkflowDefinition definition = loader.load(input);
            assertThat(definition.states()).hasSize(8).contains("CLOSED", "SUBMITTED");
            assertThat(definition.transitions()).hasSize(9);
            assertThat(definition.transitions()).extracting(WorkflowDefinition.Transition::id)
                    .containsExactly("verify-documents", "start-assessment", "approve-claim", "reject-claim",
                            "request-information", "return-with-information", "initiate-payment",
                            "close-paid-claim", "close-rejected-claim");
            assertThat(definition.guardMessages()).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                    Map.entry("all_required_documents_present", "All required documents must be present and valid before verification."),
                    Map.entry("assessor_assigned", "An assessor must be assigned before assessment can start."),
                    Map.entry("assessment_report_complete", "A complete assessment report is required before a decision."),
                    Map.entry("amount_within_policy_limit", "The requested amount must be nonnegative and no greater than the policy limit."),
                    Map.entry("rejection_reason_present", "A rejection reason is required."),
                    Map.entry("information_description_present", "A description of the missing information is required."),
                    Map.entry("new_information_received", "New information must be received before returning to verification."),
                    Map.entry("payment_request_exists", "A persisted payment request is required before payment initiation."),
                    Map.entry("payment_confirmed", "Payment must be confirmed with a reference before closure."),
                    Map.entry("appeal_period_expired_or_member_acknowledged", "The appeal period must expire or the member must acknowledge the rejection.")));
            assertThat(definition.effects()).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                    Map.entry("notify_assessor_team", "Log a notification to the assessor team."),
                    Map.entry("record_assessment_start", "Record the assessment start time."),
                    Map.entry("notify_member_approval", "Log an approval notification to the member."),
                    Map.entry("create_payment_request", "Persist a payment request."),
                    Map.entry("notify_member_rejection", "Log the reason and appeal instructions for the member."),
                    Map.entry("notify_information_request", "Log the requested information for the member."),
                    Map.entry("reset_assessment_timer", "Reset the assessment timer and clear stale completion data."),
                    Map.entry("trigger_payment_system", "Add a payment dispatch request to the local outbox."),
                    Map.entry("notify_member_payment_reference", "Log the confirmed payment reference to the member."),
                    Map.entry("archive_claim", "Archive the rejected claim after the appeal condition is met.")));
            Map<String, Set<String>> roles = definition.transitions().stream().collect(Collectors.toMap(
                    WorkflowDefinition.Transition::id, WorkflowDefinition.Transition::roles));
            assertThat(roles).containsExactlyInAnyOrderEntriesOf(Map.of(
                    "verify-documents", Set.of("document_clerk"), "start-assessment", Set.of("team_lead"),
                    "approve-claim", Set.of("assessor"), "reject-claim", Set.of("assessor"),
                    "request-information", Set.of("assessor"), "return-with-information", Set.of("document_clerk"),
                    "initiate-payment", Set.of("finance"), "close-paid-claim", Set.of("finance"),
                    "close-rejected-claim", Set.of("system")));
            assertThat(definition.cyclePolicy()).isEqualTo(new WorkflowDefinition.CyclePolicy(
                    "request-information", "informationRequests", 3,
                    "Maximum information requests exceeded — escalate to team lead"));
        }
    }

    @Test
    void rejectsDuplicateYamlKeysUnknownRolesAndBadInitialState() throws Exception {
        String yaml = resourceText();
        assertThatThrownBy(() -> load("workflow: {}\nworkflow: {}\n"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> load(yaml.replace("authorized-roles: [document_clerk]", "authorized-roles: [intruder]")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unknown roles");
        assertThatThrownBy(() -> load(yaml.replace("initial-state: SUBMITTED", "initial-state: UNKNOWN")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Initial state");
    }

    @Test
    void acceptsStateAndEdgeExtensionUsingExistingGuardAndEffect() throws Exception {
        String yaml = resourceText()
                .replace("    - CLOSED\n  guard-catalog:", "    - CLOSED\n    - MANUAL_REVIEW\n  guard-catalog:")
                .replace("  transitions:\n", "  transitions:\n    - id: extension-review\n      from: SUBMITTED\n      to: MANUAL_REVIEW\n      authorized-roles: [document_clerk]\n      guards: [all_required_documents_present]\n      effects: [notify_assessor_team]\n");
        WorkflowDefinition definition = load(yaml);
        assertThat(definition.states()).contains("MANUAL_REVIEW");
        assertThat(definition.transitions()).anySatisfy(edge -> {
            assertThat(edge.id()).isEqualTo("extension-review");
            assertThat(edge.to()).isEqualTo("MANUAL_REVIEW");
        });
        Instant at = Instant.parse("2026-10-17T12:00:00Z");
        Claim claim = new Claim(UUID.randomUUID(), "member-1", "SUBMITTED", new BigDecimal("10.00"), "USD", 0,
                new ClaimFacts(Map.of("documents", Map.of("id-card", Map.of("required", true, "present", true, "valid", true)))), at, at);
        var context = new WorkflowEvaluationContext(claim, new Actor("clerk-1", Set.of("document_clerk"), false),
                new TransitionCommand("MANUAL_REVIEW", 0, "", new ClaimFacts(Map.of())), WorkflowEvidence.empty(), at, Duration.ofDays(30));
        assertThat(new WorkflowEngine(definition, new GuardEvaluator()).evaluate(context).transitionId()).isEqualTo("extension-review");
    }

    @Test
    void rejectsUnknownStrategiesDanglingStatesAndDuplicateEdges() throws Exception {
        String yaml = resourceText();
        assertThatThrownBy(() -> load(yaml.replace("all_required_documents_present:", "unregistered_guard:")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown guard strategy");
        assertThatThrownBy(() -> load(yaml.replace("notify_assessor_team:", "unregistered_effect:")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown effect strategy");
        assertThatThrownBy(() -> load(yaml.replace("to: DOCUMENTS_VERIFIED", "to: NOT_A_STATE")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Dangling");
        assertThatThrownBy(() -> load(yaml.replace("from: DOCUMENTS_VERIFIED\n      to: UNDER_ASSESSMENT", "from: SUBMITTED\n      to: DOCUMENTS_VERIFIED")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate transition");
    }

    @Test
    void rejectsMalformedCollectionsCyclePolicyAndRoleCatalog() throws Exception {
        String yaml = resourceText();
        assertThatThrownBy(() -> load(yaml.replace("states:\n", "states: invalid\n")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> load(yaml.replace("maximum: 3", "maximum: 0")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid cycle-policy.maximum");
        assertThatThrownBy(() -> load(yaml.replace("counter: informationRequests", "counter: unknownCounter")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown cycle counter");
        assertThatThrownBy(() -> load(yaml.replace("transition-id: request-information", "transition-id: missing-transition")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cycle policy transition");
        assertThatThrownBy(() -> load(yaml.replace("authorized-roles: [document_clerk]", "authorized-roles: []")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing or unknown roles");
        assertThatThrownBy(() -> load(yaml.replace("information-cycle-limit: 3", "information-cycle-limit: 4")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must match");
    }

    @Test
    void rejectsDuplicateTransitionIdsAndDuplicateValues() throws Exception {
        String yaml = resourceText();
        assertThatThrownBy(() -> load(yaml.replace("id: start-assessment", "id: verify-documents")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate transition");
        assertThatThrownBy(() -> load(yaml.replace("authorized-roles: [document_clerk]",
                "authorized-roles: [document_clerk, document_clerk]")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate value");
    }

    @Test
    void rejectsNonpositiveAppealDurationInEvaluationContext() {
        Instant at = Instant.parse("2026-10-17T12:00:00Z");
        Claim claim = new Claim(UUID.randomUUID(), "member-1", "SUBMITTED", new BigDecimal("1.00"), "USD", 0,
                new ClaimFacts(Map.of()), at, at);
        assertThatThrownBy(() -> new WorkflowEvaluationContext(claim, new Actor("a", Set.of("system"), true),
                new TransitionCommand("CLOSED", 0, "", new ClaimFacts(Map.of())), WorkflowEvidence.empty(), at, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("positive");
        assertThatThrownBy(() -> new WorkflowEvaluationContext(claim, new Actor("a", Set.of("system"), true),
                new TransitionCommand("CLOSED", 0, "", new ClaimFacts(Map.of())), WorkflowEvidence.empty(), at, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("positive");
    }

    @Test
    void startsWorkflowBeansAndBindsConfiguredAppealPeriod() {
        new ApplicationContextRunner().withUserConfiguration(WorkflowConfiguration.class)
                .withPropertyValues("claims.workflow.appeal-period=PT48H")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(WorkflowDefinition.class)
                            .hasSingleBean(WorkflowEngine.class)
                            .hasSingleBean(GuardEvaluator.class)
                            .hasSingleBean(EffectRegistry.class);
                    assertThat(context.getBean(Duration.class)).isEqualTo(Duration.ofHours(48));
                    assertThat(context.getBean(WorkflowDefinition.class).transitions()).hasSize(9);
                });
    }

    @Test
    void rejectsInvalidAppealPeriodAndMissingWorkflowResourceAtConfigurationBoundary() {
        new ApplicationContextRunner().withUserConfiguration(WorkflowConfiguration.class)
                .withPropertyValues("claims.workflow.appeal-period=PT0S")
                .run(context -> assertThat(context).hasFailed());
        new ApplicationContextRunner().withUserConfiguration(WorkflowConfiguration.class)
                .withPropertyValues("claims.workflow.definition=classpath:missing-workflow.yml")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsNonObjectCatalogEntriesAndNonintegralCycleLimits() throws Exception {
        String yaml = resourceText();
        assertThatThrownBy(() -> load(yaml.replace("failure-message: All required documents must be present and valid before verification.",
                "failure-message: [not, a, string]")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> load(yaml.replace("maximum: 3", "maximum: 3.5")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid cycle-policy.maximum");
        assertThatThrownBy(() -> load(yaml.replace("maximum: 3", "maximum: 2147483648")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid cycle-policy.maximum");
        assertThatThrownBy(() -> load(yaml.replace("maximum: 3", "maximum: \"3\"")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cycle-policy.maximum");
        assertThatThrownBy(() -> load(yaml.replace("version: challenge-14-v1", "version: '  '")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Expected nonblank string");
    }

    @Test
    void rejectsNullMalformedYamlAndWrongTopLevelOrTransitionCollectionShapes() throws Exception {
        assertThatThrownBy(() -> loader.load(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> load("workflow: [" )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid workflow configuration");
        assertThatThrownBy(() -> load("states: []" )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing/invalid workflow");
        assertThatThrownBy(() -> load(resourceText().replace("transitions:\n", "transitions: invalid\n")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid workflow configuration");
    }

    private WorkflowDefinition load(String yaml) {
        return loader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    private String resourceText() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/workflow.yml")) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
