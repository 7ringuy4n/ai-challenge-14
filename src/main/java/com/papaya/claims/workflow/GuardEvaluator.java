package com.papaya.claims.workflow;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.function.Predicate;

public final class GuardEvaluator {
    private static final Pattern DOCUMENT_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,119}");
    private final Map<String, Predicate<WorkflowEvaluationContext>> strategies = Map.ofEntries(
            Map.entry("all_required_documents_present", this::allRequiredDocumentsPresent),
            Map.entry("assessor_assigned", c -> nonblank(c.claim().facts().values().get("assessorId"))),
            Map.entry("assessment_report_complete", c -> reportComplete(c.claim().facts().values().get("assessmentReport"))),
            Map.entry("amount_within_policy_limit", c -> amountWithin(
                    c.claim().facts().values().get("requestedAmount"), c.claim().policyLimit())),
            Map.entry("rejection_reason_present", c -> nonblank(c.command().reason())),
            Map.entry("information_description_present", c -> nonblank(c.command().reason())),
            Map.entry("new_information_received", this::newInformationReceived),
            Map.entry("payment_request_exists", c -> c.evidence().paymentRequestExists()),
            Map.entry("payment_confirmed", this::paymentConfirmed),
            Map.entry("appeal_period_expired_or_member_acknowledged", this::appealResolved));

    public Set<String> strategyIds() {
        return strategies.keySet();
    }

    public boolean passes(String id, WorkflowEvaluationContext c) {
        Predicate<WorkflowEvaluationContext> strategy = strategies.get(id);
        return strategy != null && strategy.test(c);
    }

    private boolean allRequiredDocumentsPresent(WorkflowEvaluationContext context) {
        return documentsValid(context.claim().facts().values());
    }

    private boolean newInformationReceived(WorkflowEvaluationContext context) {
        WorkflowEvidence evidence = context.evidence();
        return evidence.lastInformationRequestedAt() != null && evidence.lastInformationReceivedAt() != null
                && evidence.lastInformationReceivedAt().isAfter(evidence.lastInformationRequestedAt());
    }

    private boolean paymentConfirmed(WorkflowEvaluationContext context) {
        WorkflowEvidence evidence = context.evidence();
        return evidence.paymentRequestExists() && evidence.paymentConfirmedAt() != null
                && nonblank(evidence.paymentReference());
    }

    private boolean appealResolved(WorkflowEvaluationContext context) {
        WorkflowEvidence evidence = context.evidence();
        return evidence.rejectedAt() != null &&
                ((evidence.memberAcknowledgedRejectionAt() != null
                        && !evidence.memberAcknowledgedRejectionAt().isBefore(evidence.rejectedAt()))
                        || !context.now().isBefore(evidence.rejectedAt().plus(context.appealPeriod())));
    }
    private static boolean nonblank(Object o) { return o instanceof String s && !s.isBlank(); }
    private static boolean amountWithin(Object o, BigDecimal limit) {
        if (!(o instanceof BigDecimal b)) return false;
        return b.signum() >= 0 && b.scale() <= 2 && b.precision() - b.scale() <= 16 && b.compareTo(limit) <= 0;
    }
    private static boolean reportComplete(Object raw) {
        if (!(raw instanceof Map<?, ?> report)) return false;
        return Boolean.TRUE.equals(report.get("complete"));
    }
    private static boolean documentsValid(Map<String,Object> f) {
        Object raw=f.get("documents"); if (!(raw instanceof Map<?,?> docs) || docs.isEmpty()) return false;
        boolean hasRequired = false;
        for (var entry:docs.entrySet()) {
            if (!(entry.getKey() instanceof String id) || !DOCUMENT_ID.matcher(id).matches() || !(entry.getValue() instanceof Map<?,?> d)) return false;
            Object required = d.get("required"), present = d.get("present"), valid = d.get("valid");
            if (!(required instanceof Boolean) || !(present instanceof Boolean) || !(valid instanceof Boolean)) return false;
            if (Boolean.TRUE.equals(required)) {
                hasRequired = true;
                if (!Boolean.TRUE.equals(present) || !Boolean.TRUE.equals(valid)) return false;
            }
        }
        return hasRequired;
    }
}
