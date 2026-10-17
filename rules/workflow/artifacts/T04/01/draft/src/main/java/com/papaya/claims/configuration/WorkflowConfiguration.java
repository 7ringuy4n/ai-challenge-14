package com.papaya.claims.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.*;
import com.papaya.claims.application.*;
import com.papaya.claims.engine.ConfiguredWorkflowEngine;
import com.papaya.claims.engine.WorkflowLoader;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableScheduling
public class WorkflowConfiguration {
    @Bean Clock workflowClock() {
        Clock utc = Clock.system(ZoneOffset.UTC);
        return new MicrosecondClock(utc);
    }
    @Bean WorkflowEngine workflowEngine(@Value("${claims.workflow-path:config/workflow.yml}") String path) {
        return new ConfiguredWorkflowEngine(WorkflowLoader.load(Path.of(path)));
    }
    @Bean ClaimOperations claimOperations(ClaimRepository claims, AuditRepository audits, CommandRepository commands,
            EffectRepository effects, WorkflowEngine engine, TransactionTemplate transactions, Clock clock,
            MockEffectFactory factory, EffectDispatcher dispatcher, ObjectMapper json) {
        return new ClaimService(claims, audits, commands, effects, engine, transactions, clock, factory, dispatcher, json);
    }
    private static final class MicrosecondClock extends Clock {
        private final Clock delegate;
        private MicrosecondClock(Clock delegate) { this.delegate = delegate; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return new MicrosecondClock(delegate.withZone(zone)); }
        @Override public Instant instant() { return delegate.instant().truncatedTo(ChronoUnit.MICROS); }
    }
}
