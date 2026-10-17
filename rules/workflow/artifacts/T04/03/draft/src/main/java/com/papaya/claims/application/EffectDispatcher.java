package com.papaya.claims.application;

import com.papaya.claims.contract.EffectRepository;
import com.papaya.claims.contract.Model.OutboxMessage;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Polls committed outbox rows and retries console effects with at-least-once delivery semantics. */
@Component
public final class EffectDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(EffectDispatcher.class);
    private final EffectRepository repository;
    private final MockEffectFactory factory;
    private final Clock clock;
    private final TransactionTemplate freshTransactions;

    public EffectDispatcher(EffectRepository repository, MockEffectFactory factory, Clock clock,
                            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.factory = factory;
        this.clock = clock;
        this.freshTransactions = new TransactionTemplate(transactionManager);
        this.freshTransactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedDelayString = "${claims.outbox-retry-delay-ms:5000}")
    public void retryPending() {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Outbox polling cannot run inside an ambient transaction");
        List<OutboxMessage> ready = freshTransactions.execute(status -> repository.pending());
        if (ready != null) ready.forEach(this::deliver);
    }

    private void deliver(OutboxMessage message) {
        try {
            // Delivery happens outside every database transaction and only after polling committed rows.
            factory.deliver(message);
            freshTransactions.executeWithoutResult(status -> repository.markDelivered(message.id()));
        } catch (RuntimeException failure) {
            int seconds = (int) Math.min(3600, 1L << Math.min(message.attempts(), 10));
            try {
                freshTransactions.executeWithoutResult(status -> repository.markFailed(message.id(),
                        failure.getClass().getSimpleName(), clock.instant().plus(Duration.ofSeconds(seconds))));
            } catch (RuntimeException persistenceFailure) {
                LOG.error("Unable to persist outbox retry state id={}", message.id(), persistenceFailure);
            }
            LOG.warn("Mock effect delivery failed id={} attempt={}", message.id(), message.attempts() + 1);
        }
    }
}
