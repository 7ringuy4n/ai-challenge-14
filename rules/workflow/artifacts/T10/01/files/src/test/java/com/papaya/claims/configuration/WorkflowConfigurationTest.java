package com.papaya.claims.configuration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.application.ClaimService;
import com.papaya.claims.application.MockEffectFactory;
import com.papaya.claims.contract.*;
import java.nio.file.Path;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

class WorkflowConfigurationTest {
    private final WorkflowConfiguration configuration = new WorkflowConfiguration();

    @Test void workflowClockUsesUtcAndTickPrecisionCompatibleWithDatabaseMicroseconds() {
        Clock clock = configuration.workflowClock();
        assertThat(clock.getZone()).isEqualTo(java.time.ZoneOffset.UTC);
        assertThat(clock.instant().getNano() % 1_000).isZero();
    }

    @Test void loadsWorkflowFromConfiguredPathAndFailsWithPathForMissingConfiguration() {
        assertThat(configuration.workflowEngine("config/workflow.yml").initialState()).isEqualTo("SUBMITTED");
        assertThatThrownBy(() -> configuration.workflowEngine("config/no-such-workflow.yml"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("config/no-such-workflow.yml");
    }

    @Test void claimOperationsBeanUsesTheProvidedPortsAndEngine() {
        ClaimRepository claims = mock(ClaimRepository.class);
        AuditRepository audits = mock(AuditRepository.class);
        CommandRepository commands = mock(CommandRepository.class);
        EffectRepository effects = mock(EffectRepository.class);
        WorkflowEngine engine = configuration.workflowEngine(Path.of("config/workflow.yml").toString());
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        Clock clock = configuration.workflowClock();
        MockEffectFactory factory = mock(MockEffectFactory.class);
        ObjectMapper json = new ObjectMapper();

        ClaimOperations operations = configuration.claimOperations(claims, audits, commands, effects, engine, transactions, clock, factory, json);
        assertThat(operations).isInstanceOf(ClaimService.class);
    }
}
