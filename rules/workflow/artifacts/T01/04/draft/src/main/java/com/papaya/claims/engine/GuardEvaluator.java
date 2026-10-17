package com.papaya.claims.engine;

import com.papaya.claims.contract.Model.ClaimSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Evaluates guard primitives without expressions or reflection. */
public final class GuardEvaluator {
    public record Failure(String type, String field, String message) {}
    public List<Failure> failures(List<WorkflowDefinition.Guard> guards, ClaimSnapshot claim, Instant now) {
        Map<String,Object> facts = facts(claim, now);
        var failures = new ArrayList<Failure>();
        for (var guard : guards) if (!matches(guard, facts)) failures.add(new Failure(guard.type(), guard.field(),
                guard.message() == null || guard.message().isBlank() ? defaultMessage(guard) : guard.message()));
        return List.copyOf(failures);
    }
    private boolean matches(WorkflowDefinition.Guard g, Map<String,Object> f) {
        Object value = f.get(g.field());
        try {
            return switch (g.type()) {
                case "present" -> value != null && (!(value instanceof String s) || !s.isBlank()) && (!(value instanceof Collection<?> c) || !c.isEmpty());
                case "isTrue" -> Boolean.TRUE.equals(value);
                case "lessOrEqual" -> number(value).compareTo(number(g.otherField() == null ? g.value() : f.get(g.otherField()))) <= 0;
                case "equal" -> equal(g.otherField() == null ? value : value, g.otherField() == null ? g.value() : f.get(g.otherField()));
                case "after" -> instant(value).isAfter(instant(g.otherField() == null ? g.value() : f.get(g.otherField())));
                case "beforeOrEqual" -> !instant(value).isAfter(instant(g.otherField() == null ? g.value() : f.get(g.otherField())));
                case "allDocumentsValid" -> documentsValid(f);
                case "any" -> g.guards().stream().anyMatch(child -> matches(child, f));
                default -> false;
            };
        } catch (RuntimeException badFact) { return false; }
    }
    private static boolean equal(Object a, Object b) {
        if (a == null || b == null) return false;
        try { if (a instanceof Number || b instanceof Number) return number(a).compareTo(number(b)) == 0; }
        catch (RuntimeException ignored) { return false; }
        return a.equals(b);
    }
    private static boolean documentsValid(Map<String,Object> facts) {
        Object required = facts.get("requiredDocuments"), docs = facts.get("documents");
        if (!(required instanceof List<?> list) || list.isEmpty() || !(docs instanceof Map<?,?> map)) return false;
        return list.stream().allMatch(key -> key instanceof String s && Boolean.TRUE.equals(map.get(s)));
    }
    private static BigDecimal number(Object n) {
        if (n instanceof BigDecimal d) return d;
        if (n instanceof Number || n instanceof String) return new BigDecimal(n.toString());
        throw new IllegalArgumentException("not numeric");
    }
    private static Instant instant(Object v) {
        Instant timestamp = v instanceof Instant i ? i : Instant.parse((String) v);
        return normalize(timestamp);
    }
    static Instant normalize(Instant timestamp) { return timestamp.truncatedTo(ChronoUnit.MICROS); }
    private static Map<String,Object> facts(ClaimSnapshot claim, Instant now) {
        var result = new HashMap<String,Object>(claim.facts());
        result.put("policyLimit", claim.policyLimit()); result.put("currency", claim.currency());
        result.put("lastInformationRequestedAt", claim.lastInformationRequestedAt()); result.put("now", normalize(now));
        return result;
    }
    private static String defaultMessage(WorkflowDefinition.Guard g) { return "Guard " + g.type() + " failed" + (g.field() == null ? "" : " for " + g.field()); }
}
