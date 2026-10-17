package com.papaya.claims.engine;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Loads and validates the safe YAML workflow DSL. */
public final class WorkflowLoader {
    private static final Set<String> GUARDS = Set.of("present", "isTrue", "lessOrEqual", "equal",
            "after", "beforeOrEqual", "allDocumentsValid", "any");
    private WorkflowLoader() {}

    public static WorkflowDefinition load(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            var mapper = new ObjectMapper(new YAMLFactory()).enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            Raw raw = mapper.readValue(path.toFile(), Raw.class);
            validate(raw, path.toString());
            var transitions = raw.transitions.stream().map(t -> new WorkflowDefinition.Transition(
                    t.from, t.to, t.role, convert(t.guards), t.effects, t.informationRequest,
                    WorkflowDefinition.TimerAction.valueOf(t.timerAction == null ? "NONE" : t.timerAction), t.clearFacts)).toList();
            return new WorkflowDefinition(raw.version, raw.initialState, raw.states, raw.roles,
                    raw.maxInformationRequests, transitions);
        } catch (IOException | RuntimeException error) {
            if (error instanceof IllegalArgumentException iae && iae.getMessage() != null && iae.getMessage().startsWith(path + ":")) throw iae;
            throw new IllegalArgumentException(path + ": invalid workflow configuration: " + error.getMessage(), error);
        }
    }

    private static List<WorkflowDefinition.Guard> convert(List<RawGuard> guards) {
        if (guards == null) return List.of();
        return guards.stream().map(g -> new WorkflowDefinition.Guard(g.type, g.field, g.otherField,
                g.value, convert(g.guards), g.message)).toList();
    }
    private static void validate(Raw raw, String path) {
        if (raw == null) fail(path, "$", "document is empty");
        required(raw.version, path, "version"); required(raw.initialState, path, "initialState");
        if (raw.states == null || raw.states.isEmpty()) fail(path, "states", "must contain at least one state");
        if (new HashSet<>(raw.states).size() != raw.states.size()) fail(path, "states", "contains duplicate state");
        if (!raw.states.contains(raw.initialState)) fail(path, "initialState", "must name a configured state");
        if (raw.roles == null || raw.roles.isEmpty() || new HashSet<>(raw.roles).size() != raw.roles.size()) fail(path, "roles", "must contain unique roles");
        if (raw.maxInformationRequests < 0) fail(path, "maxInformationRequests", "must be nonnegative");
        if (raw.transitions == null) fail(path, "transitions", "is required");
        var edges = new HashSet<String>();
        for (int i = 0; i < raw.transitions.size(); i++) {
            RawTransition t = raw.transitions.get(i); String at = "transitions[" + i + "]";
            if (t == null) fail(path, at, "must not be null");
            required(t.from, path, at + ".from"); required(t.to, path, at + ".to"); required(t.role, path, at + ".role");
            if (!raw.states.contains(t.from) || !raw.states.contains(t.to)) fail(path, at, "from/to must name configured states");
            if (!raw.roles.contains(t.role)) fail(path, at + ".role", "must name a configured role");
            if (!edges.add(t.from + "\u0000" + t.to)) fail(path, at, "duplicate from/to edge");
            if (t.timerAction != null) try { WorkflowDefinition.TimerAction.valueOf(t.timerAction); }
            catch (IllegalArgumentException e) { fail(path, at + ".timerAction", "must be NONE, START, or RESET"); }
            if (t.effects == null) t.effects = List.of();
            for (String effect : t.effects) if (!EffectRegistry.supports(effect)) fail(path, at + ".effects", "unknown effect ID " + effect);
            if (t.clearFacts == null) t.clearFacts = List.of();
            validateGuards(t.guards, path, at + ".guards", 0);
        }
    }
    private static void validateGuards(List<RawGuard> guards, String path, String at, int depth) {
        if (depth > 12) fail(path, at, "guard nesting exceeds 12");
        if (guards == null) return;
        for (int i = 0; i < guards.size(); i++) {
            RawGuard g = guards.get(i); String field = at + "[" + i + "]";
            if (g == null || !GUARDS.contains(g.type)) fail(path, field + ".type", "unknown guard type");
            if (g.type.equals("any") && (g.guards == null || g.guards.isEmpty())) fail(path, field + ".guards", "any requires child guards");
            if (Set.of("present", "isTrue", "lessOrEqual", "after", "beforeOrEqual", "allDocumentsValid").contains(g.type) && blank(g.field)) fail(path, field + ".field", "is required for " + g.type);
            if (Set.of("lessOrEqual", "after", "beforeOrEqual").contains(g.type) && blank(g.otherField) && g.value == null) fail(path, field, "requires value or otherField");
            if (g.type.equals("equal") && blank(g.field)) fail(path, field + ".field", "is required for equal");
            validateGuards(g.guards, path, field + ".guards", depth + 1);
        }
    }
    private static void required(String v, String p, String f) { if (blank(v)) fail(p, f, "is required"); }
    private static boolean blank(String v) { return v == null || v.isBlank(); }
    private static void fail(String path, String field, String message) { throw new IllegalArgumentException(path + ": " + field + " " + message); }

    public static final class Raw {
        public String version, initialState;
        public List<String> states, roles;
        public int maxInformationRequests;
        public List<RawTransition> transitions;
    }
    public static final class RawTransition {
        public String from, to, role, timerAction;
        public List<RawGuard> guards;
        public List<String> effects, clearFacts;
        public boolean informationRequest;
    }
    public static final class RawGuard {
        public String type, field, otherField, message;
        public Object value;
        public List<RawGuard> guards;
    }
}
