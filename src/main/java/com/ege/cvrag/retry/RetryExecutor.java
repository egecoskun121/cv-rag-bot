package com.ege.cvrag.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Runs an action with retries using exponential backoff and full jitter.
 *
 * Backoff grows as {@code base * 2^(attempt-1)}, capped at {@code maxDelay}.
 * "Full jitter" then picks a random wait in {@code [0, backoff]} — this spreads
 * out concurrent retries so callers don't all wake up and hammer a recovering
 * service at the same instant (the "thundering herd" problem).
 */
@Component
public class RetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);

    private final int maxAttempts;
    private final long baseDelayMs;
    private final long maxDelayMs;

    public RetryExecutor(@Value("${app.ollama.retry.max-attempts}") int maxAttempts,
                         @Value("${app.ollama.retry.base-delay-ms}") long baseDelayMs,
                         @Value("${app.ollama.retry.max-delay-ms}") long maxDelayMs) {
        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
    }

    /**
     * Executes {@code action}, retrying only when {@code retryable} accepts the
     * thrown exception. Non-retryable exceptions and the final failure propagate.
     */
    public <T> T execute(Supplier<T> action, Predicate<RuntimeException> retryable) {
        int attempt = 1;
        while (true) {
            try {
                return action.get();
            } catch (RuntimeException ex) {
                if (attempt >= maxAttempts || !retryable.test(ex)) {
                    throw ex;
                }
                long backoff = backoffWithJitter(attempt);
                log.warn("Attempt {}/{} failed ({}); retrying in {} ms",
                        attempt, maxAttempts, ex.getClass().getSimpleName(), backoff);
                sleep(backoff);
                attempt++;
            }
        }
    }

    private long backoffWithJitter(int attempt) {
        long exponential = Math.min(maxDelayMs, baseDelayMs * (1L << (attempt - 1)));
        return ThreadLocalRandom.current().nextLong(exponential + 1); // full jitter: [0, exp]
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry wait interrupted", e);
        }
    }
}
