package com.papaya.claims.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.BusinessException;
import com.papaya.claims.contract.Model.*;
import com.papaya.claims.engine.EffectRegistry;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Factory selecting a payload strategy for each configured effect ID. */
@Component
public final class MockEffectFactory {
    private static final Logger LOG = LoggerFactory.getLogger(MockEffectFactory.class);
    private final ObjectMapper json;
    private final Map<String, EffectHandler> handlers;
    public MockEffectFactory(ObjectMapper json) {
        this.json = json;
        var map = new LinkedHashMap<String, EffectHandler>();
        map.put("notify_assessors", (c,a,n) -> payload("assessor_queue", c, Map.of("recipientRole", "assessor")));
        map.put("assessment_started", (c,a,n) -> payload("assessment_started", c, Map.of("recipientRole", "assessor", "assessorId", safe(c.facts().get("assessorId")))));
        map.put("notify_approval", (c,a,n) -> payload("approval_notice", c, Map.of("recipientRole", "member", "memberId", c.memberId(), "paymentReference", "pending:" + c.id())));
        map.put("notify_rejection", (c,a,n) -> payload("rejection_notice", c, Map.of("recipientRole", "member", "memberId", c.memberId(), "reason", safe(c.facts().get("rejectionReason")), "appealInstructions", "Contact the claims team before the appeal deadline.")));
        map.put("request_information", (c,a,n) -> payload("information_request", c, Map.of("recipientRole", "member", "memberId", c.memberId(), "description", safe(c.facts().get("missingInfoDescription")))));
        map.put("assessment_timer_reset", (c,a,n) -> payload("assessment_timer_reset", c, Map.of("recipientRole", "assessor")));
        map.put("trigger_payment", (c,a,n) -> payload("payment_trigger", c, Map.of("recipientRole", "finance", "paymentReference", safe(c.facts().get("paymentReference")))));
        map.put("notify_payment", (c,a,n) -> payload("payment_notice", c, Map.of("recipientRole", "member", "memberId", c.memberId(), "paymentReference", safe(c.facts().get("paymentReference")))));
        map.put("archive_claim", (c,a,n) -> payload("claim_archive", c, Map.of("recipientRole", "records")));
        handlers = Collections.unmodifiableMap(map);
        if (!handlers.keySet().equals(EffectRegistry.effectIds().stream().filter(id -> !id.equals("create_payment_request")).collect(java.util.stream.Collectors.toSet())))
            throw new IllegalStateException("Mock effect strategies do not match the engine registry");
    }
    public boolean supports(String id) { return "create_payment_request".equals(id) || handlers.containsKey(id); }
    public EffectHandler strategy(String id) {
        EffectHandler handler = handlers.get(id);
        if (handler == null) throw new BusinessException("UNKNOWN_EFFECT", 500, "No asynchronous strategy is registered for effect " + id);
        return handler;
    }
    public OutboxMessage create(String id, ClaimSnapshot claim, Actor actor, String notes) {
        return new OutboxMessage(UUID.randomUUID(), claim.id(), id, strategy(id).payload(claim, actor, notes), 0);
    }
    public void deliver(OutboxMessage message) {
        if (!supports(message.effect()) || "create_payment_request".equals(message.effect())) throw new IllegalArgumentException("Unsupported asynchronous effect");
        // Payload is intentionally not logged: it can contain member-facing claim details.
        LOG.info("mock_effect effect={} claimId={} messageId={}", message.effect(), message.claimId(), message.id());
    }
    private OutboxMessage payload(String kind, ClaimSnapshot claim, Map<String,Object> data) {
        var safe = new LinkedHashMap<String,Object>(); safe.put("kind", kind); safe.put("claimId", claim.id()); safe.putAll(data);
        try { return new OutboxMessage(UUID.randomUUID(), claim.id(), kindToEffect(kind), json.writeValueAsString(safe), 0); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Unable to encode effect payload", e); }
    }
    private static Object safe(Object value) { return value instanceof String s && s.length() <= 2000 ? s : "unavailable"; }
    private static String kindToEffect(String kind) {
        return switch (kind) {
            case "assessor_queue" -> "notify_assessors"; case "assessment_started" -> "assessment_started";
            case "approval_notice" -> "notify_approval"; case "rejection_notice" -> "notify_rejection";
            case "information_request" -> "request_information"; case "assessment_timer_reset" -> "assessment_timer_reset";
            case "payment_trigger" -> "trigger_payment"; case "payment_notice" -> "notify_payment";
            case "claim_archive" -> "archive_claim"; default -> throw new IllegalArgumentException("Unknown effect kind");
        };
    }
    @FunctionalInterface public interface EffectHandler { String payload(ClaimSnapshot claim, Actor actor, String notes); }
}
