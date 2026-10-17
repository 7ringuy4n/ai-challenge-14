package com.papaya.claims.engine;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

/** Immutable, validated workflow configuration. */
public record WorkflowDefinition(String version, String initialState, List<String> states,
                                 List<String> roles, int maxInformationRequests,
                                 List<Transition> transitions) {
    public WorkflowDefinition {
        states = List.copyOf(states);
        roles = List.copyOf(roles);
        transitions = List.copyOf(transitions);
    }
    public record Transition(String from, String to, String role, List<Guard> guards,
                             List<String> effects, boolean informationRequest,
                             TimerAction timerAction, List<String> clearFacts) {
        public Transition {
            guards = List.copyOf(guards);
            effects = List.copyOf(effects);
            clearFacts = List.copyOf(clearFacts);
        }
    }
    public record Guard(String type, String field, String otherField, Object value,
                        List<Guard> guards, String message) {
        public Guard {
            guards = guards == null ? List.of() : List.copyOf(guards);
            value = freeze(value);
        }
        private static Object freeze(Object input) {
            if (input instanceof Map<?,?> map) {
                var copy = new LinkedHashMap<String,Object>();
                map.forEach((k,v) -> copy.put(String.valueOf(k), freeze(v)));
                return Collections.unmodifiableMap(copy);
            }
            if (input instanceof List<?> list) {
                var copy = new ArrayList<Object>();
                list.forEach(v -> copy.add(freeze(v)));
                return Collections.unmodifiableList(copy);
            }
            return input;
        }
    }
    public enum TimerAction { NONE, START, RESET }
}
