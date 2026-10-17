package com.papaya.claims.workflow;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record WorkflowDefinition(String version, String initialState, Set<String> states,
        List<Transition> transitions, Map<String,String> guardMessages, Map<String,String> effects,
        CyclePolicy cyclePolicy) {
    public WorkflowDefinition {
        states = Set.copyOf(states);
        transitions = List.copyOf(transitions);
        guardMessages = Map.copyOf(guardMessages);
        effects = Map.copyOf(effects);
    }
    public record CyclePolicy(String transitionId, String counter, int maximum, String errorMessage) { }
    public record Transition(String id, String from, String to, Set<String> roles, List<String> guards, List<String> effects) {
        public Transition { roles=Set.copyOf(roles); guards=List.copyOf(guards); effects=List.copyOf(effects); }
    }
}
