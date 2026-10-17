package com.papaya.claims.workflow;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Registry of pure effect planners; external work is dispatched only after commit. */
public final class EffectRegistry {
    private final Map<String, Function<WorkflowEvaluationContext, EffectIntent>> strategies;

    public EffectRegistry() {
        Map<String, Function<WorkflowEvaluationContext, EffectIntent>> registered = new LinkedHashMap<>();
        registered.put("notify_assessor_team", c -> plan("notify_assessor_team", "claimId", c.claim().id().toString()));
        registered.put("record_assessment_start", c -> plan("record_assessment_start", "occurredAt", c.now().toString()));
        registered.put("notify_member_approval", c -> plan("notify_member_approval", "claimId", c.claim().id().toString()));
        registered.put("create_payment_request", c -> new EffectIntent("create_payment_request", Map.of(
                "claimId", c.claim().id().toString(), "currency", c.claim().currency(),
                "amount", c.claim().facts().values().get("requestedAmount"))));
        registered.put("notify_member_rejection", c -> plan("notify_member_rejection", "reason", c.command().reason()));
        registered.put("notify_information_request", c -> plan("notify_information_request", "description", c.command().reason()));
        registered.put("reset_assessment_timer", c -> plan("reset_assessment_timer", "occurredAt", c.now().toString()));
        registered.put("trigger_payment_system", c -> plan("trigger_payment_system", "claimId", c.claim().id().toString()));
        registered.put("notify_member_payment_reference", c -> plan("notify_member_payment_reference", "reference", c.evidence().paymentReference()));
        registered.put("archive_claim", c -> plan("archive_claim", "claimId", c.claim().id().toString()));
        strategies = Map.copyOf(registered);
    }

    public Set<String> strategyIds() {
        return strategies.keySet();
    }

    public boolean supports(String id) {
        return strategies.containsKey(id);
    }

    public EffectIntent resolve(String id, WorkflowEvaluationContext context) {
        Function<WorkflowEvaluationContext, EffectIntent> strategy = strategies.get(id);
        if (strategy == null) throw new IllegalArgumentException("Unknown effect strategy: " + id);
        return strategy.apply(context);
    }

    private static EffectIntent plan(String id, String key, Object value) {
        return new EffectIntent(id, Map.of(key, value));
    }
}
