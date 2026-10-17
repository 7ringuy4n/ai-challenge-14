package com.papaya.claims.web;

import com.papaya.claims.contract.ClaimOperations;
import com.papaya.claims.contract.Model.*;
import com.papaya.claims.security.AuthenticatedActors;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ClaimController {
    private final ClaimOperations operations;
    public ClaimController(ClaimOperations operations) { this.operations = operations; }

    @GetMapping("/session")
    public Session session(Authentication authentication, HttpServletRequest request) {
        Actor actor = AuthenticatedActors.from(authentication);
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (!(token instanceof CsrfToken csrf)) throw new IllegalStateException("CSRF token is unavailable");
        return new Session(actor.userId(), actor.role(), csrf.getToken(), csrf.getHeaderName());
    }

    @PostMapping("/claims")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimSnapshot create(Authentication authentication, @Valid @RequestBody CreateClaim body) {
        return operations.create(actor(authentication), body.memberId(), body.policyLimit(), body.currency(), body.requiredDocuments());
    }
    @GetMapping("/claims") public List<ClaimSnapshot> list(Authentication authentication) { return operations.list(actor(authentication)); }
    @GetMapping("/claims/{id}") public ClaimSnapshot get(Authentication authentication, @PathVariable UUID id) { return operations.get(actor(authentication), id); }
    @PatchMapping("/claims/{id}/facts") public ClaimSnapshot patchFacts(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody FactsUpdate body) {
        return operations.patchFacts(actor(authentication), id, body.expectedVersion(), body.updates());
    }
    @GetMapping("/claims/{id}/transitions") public List<TransitionOption> options(Authentication authentication, @PathVariable UUID id) { return operations.options(actor(authentication), id); }
    @PostMapping("/claims/{id}/transitions") public ClaimSnapshot transition(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody Transition body) {
        return operations.transition(actor(authentication), id, body.command());
    }
    @GetMapping("/claims/{id}/audit") public List<AuditEntry> audit(Authentication authentication, @PathVariable UUID id) { return operations.audit(actor(authentication), id); }
    @PostMapping("/system/claims/{id}/transitions") public ClaimSnapshot systemTransition(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody Transition body) {
        return operations.systemTransition(actor(authentication), id, body.command());
    }
    @PostMapping("/system/claims/{id}/close") public ClaimSnapshot close(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody Close body) {
        return operations.systemTransition(actor(authentication), id, new TransitionCommand("CLOSED", body.expectedVersion(), body.idempotencyKey(), body.notes()));
    }
    private static Actor actor(Authentication authentication) { return AuthenticatedActors.from(authentication); }

    public record Session(String userId, String role, String csrfToken, String csrfHeader) {}
    @JsonIgnoreProperties(ignoreUnknown = false)
    public record CreateClaim(@NotBlank @Size(max=120) String memberId,
            @NotNull @DecimalMin("0.00") @Digits(integer=16, fraction=2) BigDecimal policyLimit,
            @NotBlank @Pattern(regexp="[A-Z]{3}") String currency,
            @NotEmpty @Size(max=30) List<@NotBlank @Size(max=80) String> requiredDocuments) {}
    @JsonIgnoreProperties(ignoreUnknown = false)
    public record FactsUpdate(@NotNull @PositiveOrZero Long expectedVersion, @NotEmpty Map<String,Object> updates) {}
    @JsonIgnoreProperties(ignoreUnknown = false)
    public record Transition(@NotBlank @Size(max=80) String target, @NotNull @PositiveOrZero Long expectedVersion,
            @NotBlank @Size(max=120) String idempotencyKey, @Size(max=2000) String notes) {
        TransitionCommand command() { return new TransitionCommand(target, expectedVersion, idempotencyKey, notes); }
    }
    @JsonIgnoreProperties(ignoreUnknown = false)
    public record Close(@NotNull @PositiveOrZero Long expectedVersion, @NotBlank @Size(max=120) String idempotencyKey, @Size(max=2000) String notes) {}
}
