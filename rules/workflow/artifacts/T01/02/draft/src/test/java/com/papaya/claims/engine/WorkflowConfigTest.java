package com.papaya.claims.engine;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkflowConfigTest {
    @TempDir Path temp;
    @Test void loadsAllNineOriginalEdgesAndMetadata() {
        var definition = WorkflowLoader.load(Path.of("config/workflow.yml"));
        assertEquals("SUBMITTED", definition.initialState()); assertEquals(9, definition.transitions().size());
        assertEquals(3, definition.maxInformationRequests());
    }
    @Test void rejectsUnknownEffectWithPath() throws Exception {
        String yaml = Files.readString(Path.of("config/workflow.yml")).replace("notify_assessors", "magic_effect");
        var path = temp.resolve("bad.yml"); Files.writeString(path, yaml);
        var error = assertThrows(IllegalArgumentException.class, () -> WorkflowLoader.load(path));
        assertTrue(error.getMessage().contains("bad.yml")); assertTrue(error.getMessage().contains("unknown effect"));
    }
    @Test void rejectsMalformedGuardAtStartup() throws Exception {
        String yaml = Files.readString(Path.of("config/workflow.yml")).replace("type: present\n        field: assessorId", "type: eval\n        field: assessorId");
        var path = temp.resolve("bad-guard.yml"); Files.writeString(path, yaml);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> WorkflowLoader.load(path)).getMessage().contains("guards"));
    }
    @Test void rejectsEqualGuardWithoutComparisonOperandAtStartup() throws Exception {
        String yaml = Files.readString(Path.of("config/workflow.yml")).replace("        otherField: currency\n", "");
        var path = temp.resolve("missing-equality-operand.yml"); Files.writeString(path, yaml);
        var error = assertThrows(IllegalArgumentException.class, () -> WorkflowLoader.load(path));
        assertTrue(error.getMessage().contains("equal requires value or otherField"));
    }
    @Test void acceptsConfigOnlyNewStateAndExistingPrimitives() throws Exception {
        String yaml = Files.readString(Path.of("config/workflow.yml"))
                .replace("  - CLOSED\n", "  - CLOSED\n  - MANUAL_REVIEW\n")
                .replace("    effects: [notify_assessors]\n", "    effects: [notify_assessors]\n")
                .replace("  - from: REJECTED\n", "  - from: UNDER_ASSESSMENT\n    to: MANUAL_REVIEW\n    role: team_lead\n    guards: []\n    effects: [notify_assessors]\n  - from: MANUAL_REVIEW\n    to: UNDER_ASSESSMENT\n    role: team_lead\n    guards: []\n    effects: [assessment_started]\n  - from: REJECTED\n");
        var path = temp.resolve("extension.yml"); Files.writeString(path, yaml);
        var engine = new ConfiguredWorkflowEngine(WorkflowLoader.load(path));
        assertEquals("MANUAL_REVIEW", engine.options(EngineFixtures.claim("UNDER_ASSESSMENT", java.util.Map.of(), 0), new com.papaya.claims.contract.Model.Actor("lead", "team_lead"), EngineFixtures.NOW).stream().filter(o -> o.target().equals("MANUAL_REVIEW")).findFirst().orElseThrow().target());
    }
}
