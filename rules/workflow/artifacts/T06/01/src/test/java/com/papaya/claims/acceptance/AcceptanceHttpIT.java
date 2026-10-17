package com.papaya.claims.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.persistence.PostgresPersistenceSupport;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Exercises HTTP authentication/CSRF and PostgreSQL persistence through the real application. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"claims.demo-password=acceptance-human-only", "claims.system-password=acceptance-system-only"})
class AcceptanceHttpIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static PostgresPersistenceSupport.Harness harness;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        try {
            if (harness == null) harness = PostgresPersistenceSupport.start(POSTGRES);
        } catch (Exception error) {
            throw new IllegalStateException("Unable to prepare PostgreSQL acceptance database", error);
        }
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", () -> "claims_app");
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.liquibase.url", POSTGRES::getJdbcUrl);
        properties.add("spring.liquibase.user", () -> "claims_owner");
        properties.add("spring.liquibase.password", POSTGRES::getPassword);
    }

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;

    @Test
    void authenticatedHttpCreateAndAuditArePersistedInPostgres() throws Exception {
        String member = "acceptance-" + UUID.randomUUID();
        Session clerk = session("clerk", "acceptance-human-only");
        JsonNode created = exchange(clerk, HttpMethod.POST, "/api/claims", Map.of(
                "memberId", member, "policyLimit", new BigDecimal("1200.00"),
                "currency", "USD", "requiredDocuments", new String[]{"identity"}), HttpStatus.CREATED);
        UUID id = UUID.fromString(created.get("id").asText());
        assertThat(created.get("state").asText()).isEqualTo("SUBMITTED");

        JsonNode audit = exchange(clerk, HttpMethod.GET, "/api/claims/" + id + "/audit", null, HttpStatus.OK);
        assertThat(audit).isEmpty();
        assertThat(jdbc.queryForObject("SELECT state FROM claims WHERE id = ?", String.class, id))
                .isEqualTo("SUBMITTED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM claim_audit WHERE claim_id = ?", Long.class, id))
                .isZero();

        var unauthorized = http.exchange(url("/api/claims"), HttpMethod.GET,
                HttpEntity.EMPTY, String.class);
        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Session session(String user, String password) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(user, password);
        var response = http.exchange(url("/api/session"), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = json.readTree(response.getBody());
        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("XSRF-TOKEN=");
        String csrfCookie = cookie.split(";", 2)[0];
        return new Session(headers.getFirst(HttpHeaders.AUTHORIZATION), csrfCookie,
                body.get("csrfHeader").asText(), body.get("csrfToken").asText());
    }

    private JsonNode exchange(Session session, HttpMethod method, String path, Object body,
            HttpStatus expected) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, session.authorization());
        headers.set(HttpHeaders.COOKIE, session.cookie());
        headers.set(session.csrfHeader(), session.csrfToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response = http.exchange(url(path), method, new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(expected);
        return response.getBody() == null || response.getBody().isBlank()
                ? json.createObjectNode() : json.readTree(response.getBody());
    }

    private String url(String path) { return "http://localhost:" + port + path; }
    private record Session(String authorization, String cookie, String csrfHeader, String csrfToken) {}
}
