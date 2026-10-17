package com.papaya.claims.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.yaml.snakeyaml.Yaml;
import org.junit.jupiter.api.Test;

class ResourceContractTest {
    @Test
    void liquibaseMasterResolvesItsIncludedSqlAndOpenApiDefinesMutationBodies() throws IOException {
        var loader = getClass().getClassLoader();
        assertThat(loader.getResource("db/changelog/db.changelog-master.yml")).isNotNull();
        assertThat(loader.getResource("db/changelog/changes/001-initial-schema.sql")).isNotNull();

        try (var stream = loader.getResourceAsStream("db/changelog/db.changelog-master.yml")) {
            var master = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(master).contains("changes/001-initial-schema.sql");
            assertThat(master).contains("changes/002-persistence.sql");
        }
        var openApi = Files.readString(Path.of(System.getProperty("user.dir"), "docs", "api", "openapi.yaml"));
        assertThat(openApi).contains("$ref: '#/components/schemas/UpdateFactsRequest'")
                .contains("$ref: '#/components/schemas/TransitionRequest'")
                .contains("$ref: '#/components/schemas/Claim'")
                .contains("$ref: '#/components/schemas/TransitionResult'")
                .contains("policyLimit: { type: number, minimum: 0, maximum: 9999999999999999.99, multipleOf: 0.01 }")
                .contains("currency: { type: string, pattern: '^[A-Z]{3}$' }");
        assertThat(openApi).contains("/claims/{claimId}/rejection-acknowledgement:")
                .contains("/claims/{claimId}/transitions:")
                .contains("$ref: '#/components/schemas/WorkflowOption'");
    }

    @Test
    void workflowConfigurationContainsTheEightStatesNineEdgesAndExactCycleLimit() {
        InputStream resource = getClass().getClassLoader().getResourceAsStream("config/workflow.yml");
        assertThat(resource).isNotNull();
        Map<?, ?> root;
        try (resource) {
            root = new Yaml().load(resource);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
        Map<?, ?> workflow = (Map<?, ?>) root.get("workflow");
        List<Object> states = new ArrayList<>((List<?>) workflow.get("states"));
        List<?> transitions = (List<?>) workflow.get("transitions");

        assertThat(states).hasSize(8).contains("SUBMITTED", "CLOSED");
        assertThat(transitions).hasSize(9);
        List<Object> transitionIds = transitions.stream()
                .map(edge -> (Object) ((Map<?, ?>) edge).get("id"))
                .toList();
        assertThat(transitionIds)
                .containsExactly("verify-documents", "start-assessment", "approve-claim", "reject-claim",
                        "request-information", "return-with-information", "initiate-payment",
                        "close-paid-claim", "close-rejected-claim");
        Map<?, ?> cyclePolicy = (Map<?, ?>) workflow.get("cycle-policy");
        assertThat(cyclePolicy.get("maximum")).isEqualTo(3);
        assertThat(cyclePolicy.get("error-message"))
                .isEqualTo("Maximum information requests exceeded — escalate to team lead");
    }
}
