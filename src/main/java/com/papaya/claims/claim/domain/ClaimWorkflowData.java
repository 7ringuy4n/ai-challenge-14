package com.papaya.claims.claim.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Trusted workflow evidence persisted alongside a claim. */
public record ClaimWorkflowData(int informationRequests, Instant lastInformationRequestedAt,
        Instant lastInformationReceivedAt, Instant assessmentStartedAt, Instant rejectedAt,
        Instant memberAcknowledgedRejectionAt) {
    public ClaimWorkflowData {
        if (informationRequests < 0) throw new IllegalArgumentException("informationRequests cannot be negative");
        lastInformationRequestedAt = micros(lastInformationRequestedAt);
        lastInformationReceivedAt = micros(lastInformationReceivedAt);
        assessmentStartedAt = micros(assessmentStartedAt);
        rejectedAt = micros(rejectedAt);
        memberAcknowledgedRejectionAt = micros(memberAcknowledgedRejectionAt);
    }

    public static ClaimWorkflowData empty() { return new ClaimWorkflowData(0, null, null, null, null, null); }
    public Optional<Instant> lastRequested() { return Optional.ofNullable(lastInformationRequestedAt); }
    private static Instant micros(Instant value) { return value == null ? null : value.truncatedTo(java.time.temporal.ChronoUnit.MICROS); }
}
