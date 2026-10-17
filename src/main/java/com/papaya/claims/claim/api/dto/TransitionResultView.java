package com.papaya.claims.claim.api.dto;

import com.papaya.claims.claim.domain.TransitionResult;
import java.util.Objects;

/** HTTP representation of a persisted or replayed transition outcome. */
public record TransitionResultView(ClaimView claim, AuditEntryView audit, boolean replayed) {
    public TransitionResultView {
        Objects.requireNonNull(claim, "claim");
        Objects.requireNonNull(audit, "audit");
    }

    public static TransitionResultView from(TransitionResult result) {
        Objects.requireNonNull(result, "result");
        return new TransitionResultView(ClaimView.from(result.claim()), AuditEntryView.from(result.audit()), result.replayed());
    }
}
