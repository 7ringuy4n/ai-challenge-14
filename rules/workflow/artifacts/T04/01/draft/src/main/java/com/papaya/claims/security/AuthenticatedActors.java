package com.papaya.claims.security;

import com.papaya.claims.contract.Model.Actor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Converts only an authenticated Spring principal into the domain actor contract. */
public final class AuthenticatedActors {
    private AuthenticatedActors() {}
    public static Actor from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal()))
            throw new IllegalArgumentException("An authenticated principal is required");
        String role = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(value -> value.startsWith("ROLE_")).map(value -> value.substring(5).toLowerCase(java.util.Locale.ROOT))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Authenticated principal has no workflow role"));
        return new Actor(authentication.getName(), role);
    }
}
