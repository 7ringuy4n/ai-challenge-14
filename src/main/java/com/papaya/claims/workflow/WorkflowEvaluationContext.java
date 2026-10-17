package com.papaya.claims.workflow;

import com.papaya.claims.claim.domain.Actor;
import com.papaya.claims.claim.domain.Claim;
import com.papaya.claims.claim.domain.TransitionCommand;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record WorkflowEvaluationContext(Claim claim, Actor actor, TransitionCommand command,
        WorkflowEvidence evidence, Instant now, Duration appealPeriod) {
    public WorkflowEvaluationContext {
        Objects.requireNonNull(claim); Objects.requireNonNull(actor); Objects.requireNonNull(command);
        Objects.requireNonNull(evidence); now = Objects.requireNonNull(now).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        Objects.requireNonNull(appealPeriod);
        if (appealPeriod.isNegative() || appealPeriod.isZero()) throw new IllegalArgumentException("Appeal period must be positive");
    }
}
