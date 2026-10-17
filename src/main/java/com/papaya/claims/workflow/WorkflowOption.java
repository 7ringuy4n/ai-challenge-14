package com.papaya.claims.workflow;

import java.util.List;
import java.util.Set;

public record WorkflowOption(String transitionId, String from, String to, Set<String> authorizedRoles,
                             boolean available, List<String> failures, List<String> effects) {
    public WorkflowOption {
        authorizedRoles = Set.copyOf(authorizedRoles);
        failures = List.copyOf(failures);
        effects = List.copyOf(effects);
    }
}
