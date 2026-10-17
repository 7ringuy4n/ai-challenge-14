package com.papaya.claims.workflow;

import java.util.List;

public final class WorkflowEngine {
    private final WorkflowDefinition definition;
    private final GuardEvaluator guards;
    private final EffectRegistry effects;

    public WorkflowEngine(WorkflowDefinition definition, GuardEvaluator guards) {
        this(definition, guards, new EffectRegistry());
    }

    public WorkflowEngine(WorkflowDefinition definition, GuardEvaluator guards, EffectRegistry effects) {
        this.definition = definition;
        this.guards = guards;
        this.effects = effects;
        if (!effects.strategyIds().containsAll(definition.effects().keySet()))
            throw new IllegalArgumentException("Workflow definition contains an unregistered effect strategy");
    }
    public List<WorkflowOption> available(WorkflowEvaluationContext c) {
        return definition.transitions().stream().filter(t -> t.from().equals(c.claim().state())).map(t -> {
            List<String> failures = failures(t, c);
            return new WorkflowOption(t.id(), t.from(), t.to(), t.roles(), failures.isEmpty(), failures, t.effects());
        }).toList();
    }
    public WorkflowDecision evaluate(WorkflowEvaluationContext c) {
        List<WorkflowOption> opts=available(c); WorkflowDefinition.Transition t=definition.transitions().stream().filter(x->x.from().equals(c.claim().state())&&x.to().equals(c.command().targetState())).findFirst().orElse(null);
        if(t==null) {
            String choices = opts.stream().map(option -> option.transitionId() + " -> " + option.to())
                    .collect(java.util.stream.Collectors.joining(", "));
            String message = "No configured transition from " + c.claim().state() + " to "
                    + c.command().targetState() + ". Available transitions: " + (choices.isEmpty() ? "none" : choices);
            return new WorkflowDecision(false, null, c.claim().state(), c.command().targetState(),
                    "INVALID_EDGE", message, List.of(), opts);
        }
        if(!t.roles().stream().anyMatch(c.actor()::hasRole)) return WorkflowDecision.denied("ROLE_FORBIDDEN","Actor is not authorized for this transition",opts);
        for(String g:t.guards()) if(!guards.passes(g,c)) return WorkflowDecision.denied(g.toUpperCase(java.util.Locale.ROOT),definition.guardMessages().get(g),opts);
        WorkflowDefinition.CyclePolicy cycle = definition.cyclePolicy();
        if(t.id().equals(cycle.transitionId()) && c.evidence().counterValue(cycle.counter())>=cycle.maximum())
            return WorkflowDecision.denied("CYCLE_LIMIT",cycle.errorMessage(),opts);
        List<EffectIntent> plannedEffects = t.effects().stream().map(id -> effects.resolve(id, c)).toList();
        return new WorkflowDecision(true,t.id(),t.from(),t.to(),null,null,plannedEffects,opts);
    }
    private List<String> failures(WorkflowDefinition.Transition t, WorkflowEvaluationContext c) {
        List<String> failures = new java.util.ArrayList<>();
        if (t.roles().stream().noneMatch(c.actor()::hasRole)) failures.add("ROLE_FORBIDDEN");
        for (String guard : t.guards()) if (!guards.passes(guard, c)) failures.add(guard);
        WorkflowDefinition.CyclePolicy cycle = definition.cyclePolicy();
        if (t.id().equals(cycle.transitionId()) && c.evidence().counterValue(cycle.counter()) >= cycle.maximum()) failures.add("CYCLE_LIMIT");
        return List.copyOf(failures);
    }
}
