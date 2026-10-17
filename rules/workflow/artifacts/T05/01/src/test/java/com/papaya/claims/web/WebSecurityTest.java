package com.papaya.claims.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.papaya.claims.contract.ClaimOperations;
import com.papaya.claims.contract.Model.ClaimSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan;
import com.papaya.claims.observability.TraceContextFilter;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.papaya.claims.security.SecurityConfiguration;

@WebMvcTest(controllers = ClaimController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TraceContextFilter.class))
@AutoConfigureMockMvc
@Import({SecurityConfiguration.class, StrictJsonConfiguration.class})
@TestPropertySource(properties = {"claims.demo-password=temporary-test-human", "claims.system-password=temporary-test-system"})
class WebSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean ClaimOperations operations;

    @Test void apiRequiresAuthenticationAndMutationsRequireCsrf() throws Exception {
        mvc.perform(get("/api/claims")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        when(operations.create(any(), anyString(), any(), anyString(), anyList())).thenReturn(new ClaimSnapshot(UUID.randomUUID(), "member-001", "SUBMITTED", BigDecimal.ONE, "USD", Map.of(), 0, 0, null, null));
        String payload = "{\"memberId\":\"member-001\",\"policyLimit\":1,\"currency\":\"USD\",\"requiredDocuments\":[\"receipt\"]}";
        mvc.perform(post("/api/claims").with(httpBasic("clerk", "temporary-test-human")).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(post("/api/claims").with(httpBasic("clerk", "temporary-test-human")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
    }
}
