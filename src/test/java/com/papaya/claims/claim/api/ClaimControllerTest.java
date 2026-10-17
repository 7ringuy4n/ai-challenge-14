package com.papaya.claims.claim.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.papaya.claims.claim.api.dto.CreateClaimRequest;
import com.papaya.claims.claim.domain.Claim;
import com.papaya.claims.claim.domain.ClaimFacts;
import com.papaya.claims.claim.domain.AuditRecord;
import com.papaya.claims.claim.domain.PageRequest;
import com.papaya.claims.claim.domain.PageResult;
import com.papaya.claims.claim.domain.TransitionCommand;
import com.papaya.claims.claim.domain.TransitionResult;
import com.papaya.claims.claim.domain.UpdateFactsCommand;
import com.papaya.claims.claim.domain.Actor;
import com.papaya.claims.claim.service.ClaimUseCases;
import com.papaya.claims.claim.api.dto.AcknowledgeRejectionRequest;
import com.papaya.claims.claim.api.dto.TransitionRequest;
import com.papaya.claims.claim.api.dto.UpdateFactsRequest;
import com.papaya.claims.workflow.WorkflowOption;
import com.papaya.claims.common.web.CorrelationIdFilter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class ClaimControllerTest {
    @Test
    void routesEveryReadAndMutationThroughAuthenticatedWorkflowUseCases() {
        UUID claimId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-17T02:00:00Z");
        Claim claim = new Claim(claimId, "demo", "SUBMITTED", new BigDecimal("10.00"), "USD", 2,
                new ClaimFacts(Map.of("requestedAmount", new BigDecimal("3.00"))), now, now);
        AuditRecord audit = new AuditRecord(UUID.randomUUID(), claimId, 1, now, "SUBMITTED", "DOCUMENTS_VERIFIED",
                "clerk", "document_clerk", "checked", "corr-1", "v1");
        var option = new WorkflowOption("verify-documents", "SUBMITTED", "DOCUMENTS_VERIFIED",
                Set.of("document_clerk"), true, List.of(), List.of("notify_assessor_team"));
        ClaimUseCases useCases = (ClaimUseCases) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { ClaimUseCases.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "create", "get", "updateFacts", "acknowledgeRejection" -> claim;
                    case "listVisible" -> new PageResult<>(List.of(claim), 1, (PageRequest) args[0]);
                    case "options" -> List.of(option);
                    case "audit" -> new PageResult<>(List.of(audit), 1, (PageRequest) args[1]);
                    case "transition", "systemTransition" -> new TransitionResult(claim, audit, false);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var controller = new ClaimController(useCases);
        var auth = new UsernamePasswordAuthenticationToken("clerk", "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_DOCUMENT_CLERK")));
        var request = new MockHttpServletRequest("POST", "/api/v1/claims");
        request.setAttribute(CorrelationIdFilter.ATTRIBUTE, "effective-correlation");

        assertThat(controller.list(1, 5, auth).items()).hasSize(1);
        assertThat(controller.create(new CreateClaimRequest(new BigDecimal("10.00"), "USD",
                Map.of("requestedAmount", new BigDecimal("3.00"))), "create-key", request, auth)
                .getBody().id()).isEqualTo(claimId);
        assertThat(controller.get(claimId, auth).id()).isEqualTo(claimId);
        assertThat(controller.options(claimId, auth)).singleElement().satisfies(view ->
                assertThat(view.transitionId()).isEqualTo("verify-documents"));
        assertThat(controller.updateFacts(claimId, new UpdateFactsRequest(2L, Map.of("documents", Map.of())),
                "facts-key", request, auth).version()).isEqualTo(2);
        var mutation = new TransitionRequest("DOCUMENTS_VERIFIED", 2L, "checked", Map.of());
        assertThat(controller.transition(claimId, mutation, "transition-key", request, auth).audit().toState())
                .isEqualTo("DOCUMENTS_VERIFIED");
        assertThat(controller.systemTransition(claimId, mutation, "system-key", request, auth).claim().id())
                .isEqualTo(claimId);
        assertThat(controller.acknowledgeRejection(claimId, new AcknowledgeRejectionRequest(2L), "ack-key", request, auth)
                .version()).isEqualTo(2);
        assertThat(controller.audit(claimId, 0, 10, auth).items()).hasSize(1);
    }

    @Test
    void mutationUsesTheEffectiveCorrelationIdFromTheRequestFilter() {
        String effectiveId = "safe-generated-correlation-id";
        AtomicReference<String> receivedId = new AtomicReference<>();
        UUID claimId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-17T02:00:00Z");
        Claim result = new Claim(claimId, "demo", "SUBMITTED", new BigDecimal("10.00"), "USD", 0,
                new ClaimFacts(Map.of("requestedAmount", new BigDecimal("3.00"))), now, now);
        ClaimUseCases useCases = (ClaimUseCases) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { ClaimUseCases.class }, (proxy, method, args) -> {
                    if (method.getName().equals("create")) {
                        receivedId.set((String) args[3]);
                        return result;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/claims");
        request.addHeader(CorrelationIdFilter.HEADER, "invalid raw value");
        request.setAttribute(CorrelationIdFilter.ATTRIBUTE, effectiveId);
        var authentication = new UsernamePasswordAuthenticationToken("member-demo", "ignored",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        new ClaimController(useCases).create(
                new CreateClaimRequest(new BigDecimal("10.00"), "USD", Map.of("requestedAmount", new BigDecimal("3.00"))),
                "key-1", request, authentication);

        assertThat(receivedId).hasValue(effectiveId);
    }
}
