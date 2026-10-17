package com.papaya.claims.security;

import com.papaya.claims.claim.domain.Actor;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;

/** Builds workflow identity exclusively from the authenticated principal. */
public final class ActorFactory {
    private ActorFactory() { }

    public static Actor from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Authenticated principal is required");
        }
        String username = authentication.getName();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5).toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        boolean system = username.equals("system") && roles.contains("system");
        String actorId = username.startsWith("member-") ? username.substring("member-".length()) : username;
        return new Actor(actorId, roles, system);
    }
}
