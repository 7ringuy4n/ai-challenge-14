package com.papaya.claims.claim.domain;

import java.util.Objects;
import java.util.Set;

public record Actor(String id, Set<String> roles, boolean system) {
    public Actor {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Actor id is required");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        if (system && !roles.contains("system")) {
            throw new IllegalArgumentException("System actor must hold the protected system role");
        }
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
