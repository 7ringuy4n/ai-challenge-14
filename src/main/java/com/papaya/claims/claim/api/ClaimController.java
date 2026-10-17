package com.papaya.claims.claim.api;

import com.papaya.claims.claim.api.dto.*;
import com.papaya.claims.claim.domain.*;
import com.papaya.claims.claim.service.ClaimUseCases;
import com.papaya.claims.common.web.CorrelationIdFilter;
import com.papaya.claims.security.ActorFactory;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ClaimController.API_BASE)
public class ClaimController {
    public static final String API_BASE = "/api/v1";
    public static final String CLAIMS = "/claims";
    public static final String ID = "/{claimId}";
    private final ClaimUseCases claims;

    public ClaimController(ClaimUseCases claims) { this.claims = claims; }

    @GetMapping(CLAIMS)
    public PageResponse<ClaimView> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        PageResult<Claim> result = claims.listVisible(new PageRequest(page, size), ActorFactory.from(authentication));
        return PageResponse.of(result.items().stream().map(ClaimView::from).toList(), result.total(), result.request());
    }

    @PostMapping(CLAIMS)
    public ResponseEntity<ClaimView> create(@Valid @RequestBody CreateClaimRequest request,
            @RequestHeader("Idempotency-Key") String key,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        Claim claim = claims.create(new CreateClaimCommand(request.policyLimit(), request.currency(), new ClaimFacts(request.facts())),
                ActorFactory.from(authentication), key, CorrelationIdFilter.currentId(httpRequest));
        return ResponseEntity.created(URI.create(API_BASE + CLAIMS + "/" + claim.id())).body(ClaimView.from(claim));
    }

    @GetMapping(CLAIMS + ID)
    public ClaimView get(@PathVariable UUID claimId, Authentication authentication) {
        return ClaimView.from(claims.get(claimId, ActorFactory.from(authentication)));
    }

    @GetMapping(CLAIMS + ID + "/transitions")
    public java.util.List<WorkflowOptionView> options(@PathVariable UUID claimId, Authentication authentication) {
        return claims.options(claimId, ActorFactory.from(authentication)).stream().map(WorkflowOptionView::from).toList();
    }

    @PutMapping(CLAIMS + ID + "/facts")
    public ClaimView updateFacts(@PathVariable UUID claimId, @Valid @RequestBody UpdateFactsRequest request,
            @RequestHeader("Idempotency-Key") String key,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        Claim claim = claims.updateFacts(claimId, new UpdateFactsCommand(request.expectedVersion(), new ClaimFacts(request.facts())),
                ActorFactory.from(authentication), key, CorrelationIdFilter.currentId(httpRequest));
        return ClaimView.from(claim);
    }

    @PostMapping(CLAIMS + ID + "/transitions")
    public TransitionResultView transition(@PathVariable UUID claimId, @Valid @RequestBody TransitionRequest request,
            @RequestHeader("Idempotency-Key") String key,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        TransitionCommand command = new TransitionCommand(request.targetState(), request.expectedVersion(), request.reason(), new ClaimFacts(request.facts()));
        return TransitionResultView.from(claims.transition(claimId, command, ActorFactory.from(authentication), key,
                CorrelationIdFilter.currentId(httpRequest)));
    }

    @PostMapping(CLAIMS + ID + "/system-transitions")
    public TransitionResultView systemTransition(@PathVariable UUID claimId, @Valid @RequestBody TransitionRequest request,
            @RequestHeader("Idempotency-Key") String key,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        TransitionCommand command = new TransitionCommand(request.targetState(), request.expectedVersion(), request.reason(), new ClaimFacts(request.facts()));
        return TransitionResultView.from(claims.systemTransition(claimId, command, ActorFactory.from(authentication), key,
                CorrelationIdFilter.currentId(httpRequest)));
    }

    @PostMapping(CLAIMS + ID + "/rejection-acknowledgement")
    public ClaimView acknowledgeRejection(@PathVariable UUID claimId, @Valid @RequestBody AcknowledgeRejectionRequest request,
            @RequestHeader("Idempotency-Key") String key,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        return ClaimView.from(claims.acknowledgeRejection(claimId, request.expectedVersion(),
                ActorFactory.from(authentication), key, CorrelationIdFilter.currentId(httpRequest)));
    }

    @GetMapping(CLAIMS + ID + "/audit")
    public PageResponse<AuditEntryView> audit(@PathVariable UUID claimId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        PageResult<AuditRecord> result = claims.audit(claimId, new PageRequest(page, size), ActorFactory.from(authentication));
        return PageResponse.of(result.items().stream().map(AuditEntryView::from).toList(), result.total(), result.request());
    }
}
