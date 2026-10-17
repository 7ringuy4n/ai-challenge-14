package com.papaya.claims.engine;

import java.util.Set;

/** Registry capability boundary for supported effect strategies. */
public final class EffectRegistry {
    private static final Set<String> SUPPORTED = Set.of("notify_assessors", "assessment_started",
            "notify_approval", "create_payment_request", "notify_rejection", "request_information",
            "assessment_timer_reset", "trigger_payment", "notify_payment", "archive_claim");
    private EffectRegistry() {}
    public static boolean supports(String effectId) { return SUPPORTED.contains(effectId); }
    public static Set<String> effectIds() { return SUPPORTED; }
}
