package com.papaya.claims.engine;

import com.papaya.claims.contract.Model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

final class EngineFixtures {
    static final Instant NOW = Instant.parse("2026-10-16T00:00:00Z");
    static final ConfiguredWorkflowEngine ENGINE = new ConfiguredWorkflowEngine(WorkflowLoader.load(java.nio.file.Path.of("config/workflow.yml")));
    static ClaimSnapshot claim(String state, Map<String,Object> facts, int requests) {
        return new ClaimSnapshot(UUID.randomUUID(), "member-001", state, new BigDecimal("100.00"), "USD", facts,
                requests, 1, NOW.minusSeconds(600), NOW.minusSeconds(60));
    }
    static Map<String,Object> validDocs() {
        return new HashMap<>(Map.of("requiredDocuments", List.of("invoice"), "documents", Map.of("invoice", true)));
    }
    static Decision evaluate(String from, String to, String role, Map<String,Object> facts, int requests) {
        return ENGINE.evaluate(claim(from, facts, requests), new Actor("worker", role), new TransitionCommand(to, 1, "key", "note"), NOW);
    }
    static void expect(int status, String code, Runnable work) {
        var error = org.junit.jupiter.api.Assertions.assertThrows(com.papaya.claims.contract.BusinessException.class, work::run);
        org.junit.jupiter.api.Assertions.assertEquals(status, error.status());
        org.junit.jupiter.api.Assertions.assertEquals(code, error.code());
    }
    private EngineFixtures() {}
}
