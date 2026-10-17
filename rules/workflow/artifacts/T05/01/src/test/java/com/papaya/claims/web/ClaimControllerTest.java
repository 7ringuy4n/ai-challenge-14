package com.papaya.claims.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import com.papaya.claims.contract.ClaimOperations;
import com.papaya.claims.contract.Model.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClaimControllerTest {
    private ClaimOperations operations;
    private MockMvc mvc;
    private ObjectMapper json;
    private final UUID id = UUID.randomUUID();
    private UsernamePasswordAuthenticationToken clerk;
    @BeforeEach void setup() {
        operations = mock(ClaimOperations.class); json = new ObjectMapper();
        var validator = new LocalValidatorFactoryBean(); validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ClaimController(operations)).setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))).build();
        clerk = new UsernamePasswordAuthenticationToken("clerk", "n/a", List.of(new SimpleGrantedAuthority("ROLE_DOCUMENT_CLERK")));
    }
    @Test void createReturns201AndActorComesFromPrincipalRatherThanBody() throws Exception {
        ClaimSnapshot claim = new ClaimSnapshot(id, "member-001", "SUBMITTED", new BigDecimal("10.00"), "USD", Map.of(), 0, 0, null, null);
        when(operations.create(any(), anyString(), any(), anyString(), anyList())).thenReturn(claim);
        mvc.perform(post("/api/claims").principal(clerk).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"member-001\",\"policyLimit\":10,\"currency\":\"USD\",\"requiredDocuments\":[\"receipt\"],\"actor\":\"system\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(operations);
        mvc.perform(post("/api/claims").principal(clerk).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"member-001\",\"policyLimit\":10,\"currency\":\"USD\",\"requiredDocuments\":[\"receipt\"]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("SUBMITTED"));
        verify(operations).create(argThat(a -> a.userId().equals("clerk") && a.role().equals("document_clerk")), eq("member-001"), any(), eq("USD"), anyList());
    }
    @Test void malformedUuidUnknownJsonAndBeanValidationReturnSafe400() throws Exception {
        mvc.perform(get("/api/claims/not-a-uuid").principal(clerk)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.traceId").exists());
        mvc.perform(post("/api/claims").principal(clerk).contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Request body or path parameter is invalid"));
        mvc.perform(post("/api/claims").principal(clerk).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"m\",\"policyLimit\":1,\"currency\":\"USD\",\"requiredDocuments\":[\"r\"],\"extra\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/claims").principal(clerk).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"m\",\"policyLimit\":-1,\"currency\":\"usd\",\"requiredDocuments\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(operations);
    }
    @Test void humanAndProtectedSystemRoutesCallTheirDistinctOperations() throws Exception {
        var system = new UsernamePasswordAuthenticationToken("system", "n/a", List.of(new SimpleGrantedAuthority("ROLE_SYSTEM")));
        when(operations.transition(any(), eq(id), any())).thenReturn(new ClaimSnapshot(id, "m", "REVIEW", BigDecimal.ONE, "USD", Map.of(), 0, 1, null, null));
        when(operations.systemTransition(any(), eq(id), any())).thenReturn(new ClaimSnapshot(id, "m", "CLOSED", BigDecimal.ONE, "USD", Map.of(), 0, 1, null, null));
        String command = "{\"target\":\"REVIEW\",\"expectedVersion\":0,\"idempotencyKey\":\"k\",\"notes\":\"\"}";
        mvc.perform(post("/api/claims/{id}/transitions", id).principal(clerk).contentType(MediaType.APPLICATION_JSON).content(command)).andExpect(status().isOk());
        mvc.perform(post("/api/system/claims/{id}/transitions", id).principal(system).contentType(MediaType.APPLICATION_JSON).content(command)).andExpect(status().isOk());
        verify(operations).transition(argThat(a -> a.role().equals("document_clerk")), eq(id), any());
        verify(operations).systemTransition(argThat(a -> a.role().equals("system")), eq(id), any());
    }
    @Test void sessionReturnsAuthenticatedIdentityAndCsrfHeaderName() throws Exception {
        mvc = MockMvcBuilders.standaloneSetup(new ClaimController(operations)).setControllerAdvice(new GlobalExceptionHandler()).build();
        var csrf = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123");
        mvc.perform(get("/api/session").principal(clerk).requestAttr(org.springframework.security.web.csrf.CsrfToken.class.getName(), csrf))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value("clerk"));
    }
}
