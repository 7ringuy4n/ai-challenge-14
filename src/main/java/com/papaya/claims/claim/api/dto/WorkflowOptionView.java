package com.papaya.claims.claim.api.dto;

import com.papaya.claims.workflow.WorkflowOption;
import java.util.List;
import java.util.Set;

public record WorkflowOptionView(String transitionId, String from, String to, Set<String> authorizedRoles,
                                 boolean available, List<String> failures, List<String> effects) {
    public static WorkflowOptionView from(WorkflowOption option) {
        return new WorkflowOptionView(option.transitionId(), option.from(), option.to(), option.authorizedRoles(),
                option.available(), option.failures(), option.effects());
    }
}
