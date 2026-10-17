package com.papaya.claims.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.*;
import com.papaya.claims.application.*;
import com.papaya.claims.engine.ConfiguredWorkflowEngine;
import com.papaya.claims.engine.WorkflowLoader;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableScheduling
public class WorkflowConfiguration {
    @Bean Clock workflowClock() {
        return Clock.tick(Clock.systemUTC(), java.time.Duration.ofNanos(1_000));
    }
    @Bean WorkflowEngine workflowEngine(@Value("${claims.workflow-path:config/workflow.yml}") String path) {
        return new ConfiguredWorkflowEngine(WorkflowLoader.load(Path.of(path)));
    }
    @Bean ClaimOperations claimOperations(ClaimRepository claims, AuditRepository audits, CommandRepository commands,
            EffectRepository effects, WorkflowEngine engine, TransactionTemplate transactions, Clock clock,
            MockEffectFactory factory, ObjectMapper json) {
        return new ClaimService(claims, audits, commands, effects, engine, transactions, clock, factory, json);
    }
}
