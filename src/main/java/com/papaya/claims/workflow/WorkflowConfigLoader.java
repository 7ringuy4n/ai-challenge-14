package com.papaya.claims.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public final class WorkflowConfigLoader {
    private static final Set<String> SUPPORTED_ROLES = Set.of("document_clerk", "team_lead", "assessor", "finance", "system");
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private final GuardEvaluator guardRegistry;
    private final EffectRegistry effectRegistry;

    public WorkflowConfigLoader() {
        this(new GuardEvaluator(), new EffectRegistry());
    }

    public WorkflowConfigLoader(GuardEvaluator guardRegistry, EffectRegistry effectRegistry) {
        this.guardRegistry = guardRegistry;
        this.effectRegistry = effectRegistry;
    }
    public WorkflowDefinition load(InputStream input) {
        try {
            Map<?,?> root = map(mapper.readValue(input, Map.class), "root");
            Map<?,?> w = map(root.get("workflow"), "workflow");
            Set<String> states = uniqueStrings(w.get("states"), "states");
            String initialState = text(w.get("initial-state"));
            if (!states.contains(initialState)) throw bad("Initial state is not declared");
            Map<String,String> guards = new LinkedHashMap<>();
            map(w.get("guard-catalog"),"guard-catalog").forEach((k,v)-> {
                String id = text(k);
                if (!guardRegistry.strategyIds().contains(id)) throw bad("Unknown guard strategy: " + id);
                guards.put(id, text(map(v,"guard").get("failure-message")));
            });
            Map<String,String> effects = new LinkedHashMap<>();
            map(w.get("effect-catalog"),"effect-catalog").forEach((k,v)-> {
                String id = text(k);
                if (!effectRegistry.supports(id)) throw bad("Unknown effect strategy: " + id);
                effects.put(id, text(v));
            });
            List<WorkflowDefinition.Transition> ts=new ArrayList<>(); Set<String> ids=new HashSet<>(); Set<String> edges=new HashSet<>();
            for(Object o:(List<?>)w.get("transitions")) { Map<?,?> t=map(o,"transition"); String id=text(t.get("id")), from=text(t.get("from")), to=text(t.get("to"));
                if(!ids.add(id)||!edges.add(from+"\u0000"+to)) throw bad("Duplicate transition id or edge"); if(!states.contains(from)||!states.contains(to)) throw bad("Dangling transition state");
                List<String> gs=List.copyOf(uniqueStrings(t.get("guards"),"guards")), es=List.copyOf(uniqueStrings(t.get("effects"),"effects")); if(!guards.keySet().containsAll(gs)||!effects.keySet().containsAll(es)) throw bad("Transition references unknown guard or effect");
                Set<String> roles = new LinkedHashSet<>(uniqueStrings(t.get("authorized-roles"), "authorized-roles"));
                if (roles.isEmpty() || !SUPPORTED_ROLES.containsAll(roles)) throw bad("Transition has missing or unknown roles");
                ts.add(new WorkflowDefinition.Transition(id,from,to,roles,gs,es)); }
            Map<?,?> cp=map(w.get("cycle-policy"),"cycle-policy");
            String cycleTransition = text(cp.get("transition-id"));
            String counter = text(cp.get("counter"));
            int max = positiveInteger(cp.get("maximum"), "cycle-policy.maximum");
            String cycleMessage = text(cp.get("error-message"));
            if (ts.stream().noneMatch(t -> t.id().equals(cycleTransition))) throw bad("Cycle policy transition is unknown");
            if (!"informationRequests".equals(counter)) throw bad("Unknown cycle counter: " + counter);
            if (w.containsKey("information-cycle-limit") && positiveInteger(w.get("information-cycle-limit"), "information-cycle-limit") != max)
                throw bad("Information cycle limit must match cycle policy maximum");
            return new WorkflowDefinition(text(w.get("version")),initialState,states,ts,guards,effects,
                    new WorkflowDefinition.CyclePolicy(cycleTransition,counter,max,cycleMessage));
        } catch(IOException|ClassCastException|NullPointerException ex) { throw new IllegalArgumentException("Invalid workflow configuration",ex); }
    }
    private static Map<?,?> map(Object x,String label){if(!(x instanceof Map<?,?> m))throw bad("Missing/invalid "+label);return m;}
    private static List<String> strings(Object x,String label){if(!(x instanceof List<?> l))throw bad("Missing/invalid "+label);return l.stream().map(WorkflowConfigLoader::text).toList();}
    private static Set<String> uniqueStrings(Object x, String label) {
        List<String> values = strings(x, label);
        Set<String> unique = new LinkedHashSet<>(values);
        if (unique.size() != values.size()) throw bad("Duplicate value in " + label);
        return unique;
    }
    private static int positiveInteger(Object value, String label) {
        if (!(value instanceof Number number)) throw bad("Missing/invalid " + label);
        long n = number.longValue();
        if (n <= 0 || n > Integer.MAX_VALUE || number.doubleValue() != n) throw bad("Invalid " + label);
        return (int) n;
    }
    private static String text(Object x){if(!(x instanceof String s)||s.isBlank())throw bad("Expected nonblank string");return s;}
    private static IllegalArgumentException bad(String m){return new IllegalArgumentException(m);}
}
