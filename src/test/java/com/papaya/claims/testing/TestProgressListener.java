package com.papaya.claims.testing;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/** Prints deterministic test-case progress for local and CI verification logs. */
public final class TestProgressListener implements TestExecutionListener {
    private final AtomicLong started = new AtomicLong();
    private volatile long total;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        total = testPlan.countTestIdentifiers(TestIdentifier::isTest);
    }

    @Override
    public void executionStarted(TestIdentifier identifier) {
        if (identifier.isTest()) {
            long current = started.incrementAndGet();
            System.out.printf("running test case %d/%d: %s%n", current, total, identifier.getDisplayName());
        }
    }
}
