package com.papaya.claims.workflow;

import java.util.List;

public record WorkflowDecision(boolean allowed, String transitionId, String from, String to,
                               String failureCode, String failureMessage, List<EffectIntent> effects,
                               List<WorkflowOption> availableTransitions) {
    public WorkflowDecision { effects = List.copyOf(effects); availableTransitions = List.copyOf(availableTransitions); }
    public static WorkflowDecision denied(String code, String message, List<WorkflowOption> available) {
        return new WorkflowDecision(false, null, null, null, code, message, List.of(), available);
    }
}
