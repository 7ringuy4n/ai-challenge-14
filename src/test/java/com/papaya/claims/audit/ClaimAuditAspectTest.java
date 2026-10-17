package com.papaya.claims.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.papaya.claims.claim.domain.Actor;
import com.papaya.claims.claim.domain.CreateClaimCommand;
import com.papaya.claims.claim.domain.SystemOperationAuditRecord;
import com.papaya.claims.claim.domain.SystemOperationAuditRepository;
import com.papaya.claims.claim.domain.TransitionCommand;
import com.papaya.claims.claim.domain.UpdateFactsCommand;
import com.papaya.claims.claim.service.BusinessException;
import com.papaya.claims.claim.service.ClaimServiceImpl;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.junit.jupiter.api.Test;

class ClaimAuditAspectTest {
    @Test
    void selectedMutationsDeclareRuntimeAuditActionAndResource() throws Exception {
        assertMetadata("create", new Class<?>[] {CreateClaimCommand.class, Actor.class, String.class, String.class}, "claim.create");
        assertMetadata("updateFacts", new Class<?>[] {UUID.class, UpdateFactsCommand.class, Actor.class, String.class, String.class}, "claim.facts.update");
        assertMetadata("acknowledgeRejection", new Class<?>[] {UUID.class, long.class, Actor.class, String.class, String.class}, "claim.rejection.acknowledge");
        assertMetadata("transition", new Class<?>[] {UUID.class, TransitionCommand.class, Actor.class, String.class, String.class}, "claim.transition");
        assertMetadata("systemTransition", new Class<?>[] {UUID.class, TransitionCommand.class, Actor.class, String.class, String.class}, "claim.system-transition");
    }

    @Test
    void failureAdviceUsesAnnotationMetadataAndReplacesUnsafeCorrelationIds() throws Exception {
        AtomicReference<SystemOperationAuditRecord> written = new AtomicReference<>();
        SystemOperationAuditRepository repository = written::set;
        Clock clock = Clock.fixed(Instant.parse("2026-10-17T02:00:00Z"), ZoneOffset.UTC);
        ClaimFailureAuditAspect aspect = new ClaimFailureAuditAspect(new FailureAuditWriter(repository, clock));
        UUID claimId = UUID.randomUUID();
        Actor actor = new Actor("assessor", Set.of("assessor"), false);
        JoinPoint call = joinPoint(new Object[] {claimId, actor, "key", "unsafe correlation"}, "transition");
        Auditable metadata = FailureFixture.class.getDeclaredMethod("write").getAnnotation(Auditable.class);

        aspect.recordFailure(call, new BusinessException("VERSION_CONFLICT", "Stale version", 409), metadata);

        assertThat(written).hasValueSatisfying(record -> {
            assertThat(record.action()).isEqualTo("test.write");
            assertThat(record.resource()).isEqualTo("claim");
            assertThat(record.resourceId()).isEqualTo(claimId.toString());
            assertThat(record.actorId()).isEqualTo("assessor");
            assertThat(record.outcome()).isEqualTo("FAILED");
            assertThat(record.errorType()).isEqualTo("VERSION_CONFLICT");
            assertThat(record.correlationId()).matches("[0-9a-f-]{36}");
        });
    }

    @Test
    void failureAdviceHandlesMissingIdentityAndDoesNotMaskBusinessFailureWhenAuditFails() throws Exception {
        AtomicReference<SystemOperationAuditRecord> written = new AtomicReference<>();
        FailureAuditWriter writer = new FailureAuditWriter(written::set,
                Clock.fixed(Instant.parse("2026-10-17T02:00:00.123456789Z"), ZoneOffset.UTC));
        ClaimFailureAuditAspect aspect = new ClaimFailureAuditAspect(writer);
        Auditable metadata = FailureFixture.class.getDeclaredMethod("write").getAnnotation(Auditable.class);

        aspect.recordFailure(joinPoint(new Object[]{UUID.randomUUID(), new Actor("actor-1", Set.of("assessor"), false), "opaque-id"}, "transition"),
                new IllegalStateException("safe type only"), metadata);
        assertThat(written).hasValueSatisfying(record -> {
            assertThat(record.actorId()).isEqualTo("actor-1");
            assertThat(record.resourceId()).isNotNull();
            assertThat(record.errorType()).isEqualTo("IllegalStateException");
            assertThat(record.occurredAt().getNano()).isEqualTo(123_456_000);
        });

        ClaimFailureAuditAspect failingAspect = new ClaimFailureAuditAspect(new FailureAuditWriter(record -> {
            throw new IllegalStateException("audit unavailable");
        }, Clock.systemUTC()));
        assertThatCode(() -> failingAspect.recordFailure(joinPoint(new Object[]{new Actor("a", Set.of("assessor"), false)}, "transition"),
                new RuntimeException("business failure"), metadata)).doesNotThrowAnyException();
    }

    @Test
    void springAopProxyAuditsAnnotatedFailureAndPreservesTheBusinessException() {
        AtomicReference<SystemOperationAuditRecord> written = new AtomicReference<>();
        ClaimFailureAuditAspect aspect = new ClaimFailureAuditAspect(new FailureAuditWriter(written::set,
                Clock.fixed(Instant.parse("2026-10-17T02:00:00Z"), ZoneOffset.UTC)));
        AspectJProxyFactory factory = new AspectJProxyFactory(new ProxiedFailureFixture());
        factory.addAspect(aspect);
        ProxiedFailureFixture service = factory.getProxy();
        UUID claimId = UUID.randomUUID();
        Actor actor = new Actor("assessor", Set.of("assessor"), false);

        assertThatThrownBy(() -> service.reject(claimId, actor, "key", "corr-123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Original business rejection");
        assertThat(written).hasValueSatisfying(record -> {
            assertThat(record.action()).isEqualTo("claim.reject");
            assertThat(record.resource()).isEqualTo("claim");
            assertThat(record.resourceId()).isEqualTo(claimId.toString());
            assertThat(record.correlationId()).isEqualTo("corr-123");
            assertThat(record.errorType()).isEqualTo("DOMAIN_REJECTED");
        });
    }

    private static void assertMetadata(String methodName, Class<?>[] parameterTypes, String action) throws Exception {
        Method method = ClaimServiceImpl.class.getMethod(methodName, parameterTypes);
        Auditable metadata = method.getAnnotation(Auditable.class);
        assertThat(metadata).isNotNull();
        assertThat(metadata.action()).isEqualTo(action);
        assertThat(metadata.resource()).isEqualTo("claim");
    }

    private static JoinPoint joinPoint(Object[] args, String name) {
        Signature signature = (Signature) Proxy.newProxyInstance(ClaimAuditAspectTest.class.getClassLoader(),
                new Class<?>[] {Signature.class}, (proxy, method, values) -> method.getName().equals("getName") ? name : null);
        return (JoinPoint) Proxy.newProxyInstance(ClaimAuditAspectTest.class.getClassLoader(),
                new Class<?>[] {JoinPoint.class}, (proxy, method, values) -> switch (method.getName()) {
                    case "getArgs" -> args;
                    case "getSignature" -> signature;
                    default -> null;
                });
    }

    private static class FailureFixture {
        @Auditable(action = "test.write", resource = "claim")
        void write() { }
    }

    public static class ProxiedFailureFixture {
        @Auditable(action = "claim.reject", resource = "claim")
        public void reject(UUID claimId, Actor actor, String key, String correlationId) {
            throw new BusinessException("DOMAIN_REJECTED", "Original business rejection", 409);
        }
    }
}
