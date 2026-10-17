package com.papaya.claims.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopUtils;
import org.springframework.aop.framework.ProxyFactory;

class PersistenceProxyTest {
    @Test
    void repositoryAdaptersCanBeWrappedBySpringClassBasedProxies() {
        ObjectMapper json = new ObjectMapper();
        List<Object> repositories = List.of(
                new MyBatisClaimRepository(mock(ClaimMapper.class), json),
                new MyBatisAuditRepository(mock(AuditMapper.class)),
                new MyBatisCommandRepository(mock(CommandMapper.class), json),
                new MyBatisEffectRepository(mock(EffectMapper.class)));

        for (Object repository : repositories) {
            Object proxy = classProxy(repository);

            assertThat(AopUtils.isCglibProxy(proxy)).isTrue();
            assertThat(proxy).isInstanceOf(repository.getClass());
        }
    }

    private Object classProxy(Object target) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice((MethodInterceptor) invocation -> invocation.proceed());
        return factory.getProxy();
    }
}
