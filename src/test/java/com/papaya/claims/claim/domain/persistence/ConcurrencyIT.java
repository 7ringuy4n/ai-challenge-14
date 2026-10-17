package com.papaya.claims.claim.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.papaya.claims.claim.domain.*;
import com.papaya.claims.claim.domain.IdempotencyRepository.Receipt;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class ConcurrencyIT extends PersistenceITSupport {
    @Test
    void competingClaimCasUpdatesAllowOnlyOneWriter() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var repository = new MyBatisClaimRepository(claims(), JSON);
        repository.insert(new ClaimAggregate(new Claim(id, "member-cas", "submitted",
                new BigDecimal("80.00"), "USD", 0, new ClaimFacts(Map.of()), now, now), ClaimWorkflowData.empty()));
        long contestedVersion = repository.find(id).orElseThrow().claim().version();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> compete(id, contestedVersion, "assessing", now, ready, start));
            var second = pool.submit(() -> compete(id, contestedVersion, "under_review", now, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            boolean firstWon = first.get(10, TimeUnit.SECONDS);
            boolean secondWon = second.get(10, TimeUnit.SECONDS);
            assertThat((firstWon ? 1 : 0) + (secondWon ? 1 : 0)).isEqualTo(1);
            assertThat(repository.find(id).orElseThrow().claim().version()).isEqualTo(1);
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void idempotencyTransactionLockSerializesSameKeyAndExposesCommittedReceipt() throws Exception {
        String actor = "actor-concurrent";
        String scope = "claim.create";
        String key = "same-key-" + UUID.randomUUID();
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondAttempting = new CountDownLatch(1);
        CountDownLatch secondLocked = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        var transactionManager = new DataSourceTransactionManager(runtimeDataSource());
        var first = pool.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
            var repository = new MyBatisIdempotencyRepository(receipts(), JSON);
            repository.lock(actor, scope, key);
            firstLocked.countDown();
            await(releaseFirst);
            repository.insert(new Receipt(UUID.randomUUID(), actor, scope, key, "b".repeat(64), "{}"));
            return null;
        }));
        try {
            assertThat(firstLocked.await(5, TimeUnit.SECONDS)).isTrue();
            var second = pool.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                var repository = new MyBatisIdempotencyRepository(receipts(), JSON);
                secondAttempting.countDown();
                repository.lock(actor, scope, key);
                secondLocked.countDown();
                return repository.find(actor, scope, key).orElseThrow();
            }));
            assertThat(secondAttempting.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondLocked.await(250, TimeUnit.MILLISECONDS)).isFalse();
            releaseFirst.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isNull();
            assertThat(second.get(10, TimeUnit.SECONDS).idempotencyKey()).isEqualTo(key);
            assertThat(secondLocked.getCount()).isZero();
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static boolean compete(UUID id, long expectedVersion, String state, Instant now,
                                   CountDownLatch ready, CountDownLatch start) throws Exception {
        var tx = new TransactionTemplate(new DataSourceTransactionManager(runtimeDataSource()));
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("CAS start gate timed out");
        return Boolean.TRUE.equals(tx.execute(status -> {
            var repository = new MyBatisClaimRepository(claims(), JSON);
            var before = repository.find(id).orElseThrow().claim();
            var next = new ClaimAggregate(new Claim(id, before.memberId(), state, before.policyLimit(),
                    before.currency(), before.version() + 1, before.facts(), before.createdAt(), now), ClaimWorkflowData.empty());
            return repository.compareAndSet(next, expectedVersion);
        }));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("transaction release timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("transaction interrupted", e);
        }
    }
}
