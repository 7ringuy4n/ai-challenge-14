package com.papaya.claims.config;

import com.papaya.claims.workflow.GuardEvaluator;
import com.papaya.claims.workflow.EffectRegistry;
import com.papaya.claims.workflow.WorkflowConfigLoader;
import com.papaya.claims.workflow.WorkflowDefinition;
import com.papaya.claims.workflow.WorkflowEngine;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
@EnableScheduling
public class WorkflowConfiguration {
    @Bean
    GuardEvaluator guardEvaluator() {
        return new GuardEvaluator();
    }

    @Bean
    EffectRegistry effectRegistry() {
        return new EffectRegistry();
    }

    @Bean
    WorkflowDefinition workflowDefinition(
            ResourceLoader resourceLoader,
            @Value("${claims.workflow.definition:classpath:config/workflow.yml}") String location,
            GuardEvaluator guards,
            EffectRegistry effects) {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream input = resource.getInputStream()) {
            return new WorkflowConfigLoader(guards, effects).load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load workflow definition from " + location, exception);
        }
    }

    @Bean
    WorkflowEngine workflowEngine(WorkflowDefinition definition, GuardEvaluator guards, EffectRegistry effects) {
        return new WorkflowEngine(definition, guards, effects);
    }

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    Duration appealPeriod(@Value("${claims.workflow.appeal-period:P30D}") String configured) {
        Duration duration = Duration.parse(configured);
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("claims.workflow.appeal-period must be positive");
        }
        return duration;
    }
}
