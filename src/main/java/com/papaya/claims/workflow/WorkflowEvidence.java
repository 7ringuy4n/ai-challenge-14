package com.papaya.claims.workflow;

import java.time.Instant;

/** Trusted server supplied evidence. Never construct this record from client supplied facts. */
public record WorkflowEvidence(int informationRequests, Instant lastInformationRequestedAt,
        Instant lastInformationReceivedAt, Instant assessmentStartedAt, Instant rejectedAt,
        Instant memberAcknowledgedRejectionAt, boolean paymentRequestExists, String paymentReference,
        Instant paymentConfirmedAt) {
    public WorkflowEvidence {
        lastInformationRequestedAt = micros(lastInformationRequestedAt);
        lastInformationReceivedAt = micros(lastInformationReceivedAt);
        assessmentStartedAt = micros(assessmentStartedAt);
        paymentConfirmedAt = micros(paymentConfirmedAt);
        rejectedAt = micros(rejectedAt);
        memberAcknowledgedRejectionAt = micros(memberAcknowledgedRejectionAt);
        if (informationRequests < 0) throw new IllegalArgumentException("Cycle count cannot be negative");
    }
    public int counterValue(String counter) {
        if ("informationRequests".equals(counter)) return informationRequests;
        throw new IllegalArgumentException("Unsupported cycle counter: " + counter);
    }
    private static Instant micros(Instant value) { return value == null ? null : value.truncatedTo(java.time.temporal.ChronoUnit.MICROS); }
    public static WorkflowEvidence empty() { return new WorkflowEvidence(0,null,null,null,null,null,false,null,null); }
}
