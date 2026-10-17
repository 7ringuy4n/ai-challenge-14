package com.papaya.claims.engine;

import com.papaya.claims.contract.BusinessException;
import com.papaya.claims.contract.Model.*;
import com.papaya.claims.contract.WorkflowEngine;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Pure config-driven evaluator; states are opaque strings. */
public final class ConfiguredWorkflowEngine implements WorkflowEngine {
    public static final String MAX_REQUESTS_MESSAGE = "Maximum information requests exceeded — escalate to team lead";
    private final WorkflowDefinition definition;
    private final GuardEvaluator guardEvaluator = new GuardEvaluator();
    public ConfiguredWorkflowEngine(WorkflowDefinition definition) { this.definition = Objects.requireNonNull(definition); }
    @Override public String initialState() { return definition.initialState(); }
    @Override public String configVersion() { return definition.version(); }

    @Override public Decision evaluate(ClaimSnapshot claim, Actor actor, TransitionCommand command, Instant now) {
        Instant canonicalNow = GuardEvaluator.normalize(now);
        var candidates = definition.transitions().stream().filter(t -> t.from().equals(claim.state()) && t.to().equals(command.target())).toList();
        if (candidates.isEmpty()) {
            String available = definition.transitions().stream().filter(t -> t.from().equals(claim.state())).map(WorkflowDefinition.Transition::to).distinct().collect(Collectors.joining(", "));
            throw new BusinessException("INVALID_TRANSITION", 409, "Transition " + claim.state() + " → " + command.target() + " is not configured; available: " + (available.isEmpty() ? "none" : available));
        }
        var transition = candidates.getFirst();
        if (!transition.role().equals(actor.role())) throw new BusinessException("FORBIDDEN_ROLE", 403, "Transition to " + transition.to() + " requires role " + transition.role());
        if (transition.informationRequest() && claim.informationRequests() >= definition.maxInformationRequests())
            throw new BusinessException("INFORMATION_REQUEST_LIMIT", 422, MAX_REQUESTS_MESSAGE);
        var failed = guardEvaluator.failures(transition.guards(), claim, canonicalNow);
        if (!failed.isEmpty()) throw new BusinessException("GUARD_FAILED", 422, failed.stream().map(GuardEvaluator.Failure::message).collect(Collectors.joining("; ")));
        int count = claim.informationRequests() + (transition.informationRequest() ? 1 : 0);
        Instant started = switch (transition.timerAction()) {
            case START -> canonicalNow;
            case RESET -> null;
            case NONE -> claim.assessmentStartedAt();
        };
        Instant requested = transition.informationRequest() ? canonicalNow : claim.lastInformationRequestedAt();
        return new Decision(claim.state(), transition.to(), count, started, requested, transition.effects(), transition.clearFacts());
    }

    @Override public List<TransitionOption> options(ClaimSnapshot claim, Actor actor, Instant now) {
        Instant canonicalNow = GuardEvaluator.normalize(now);
        return definition.transitions().stream().filter(t -> t.from().equals(claim.state())).map(t -> {
            var blockers = new ArrayList<String>();
            if (!t.role().equals(actor.role())) blockers.add("Requires role " + t.role());
            if (t.informationRequest() && claim.informationRequests() >= definition.maxInformationRequests()) blockers.add(MAX_REQUESTS_MESSAGE);
            guardEvaluator.failures(t.guards(), claim, canonicalNow).forEach(f -> blockers.add(f.message()));
            return new TransitionOption(t.to(), t.role(), blockers.isEmpty(), blockers);
        }).toList();
    }
}
