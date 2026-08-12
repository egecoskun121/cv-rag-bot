package com.ege.cvrag.circuitbreaker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OllamaCircuitBreakerTest {

    private static final int THRESHOLD = 2;
    private static final long OPEN_WAIT_MS = 100;

    private OllamaCircuitBreaker newBreaker() {
        return new OllamaCircuitBreaker(THRESHOLD, OPEN_WAIT_MS);
    }

    @Test
    void allowsCallsWhileClosed() {
        OllamaCircuitBreaker breaker = newBreaker();
        assertThatCode(breaker::acquire).doesNotThrowAnyException();
    }

    @Test
    void opensAfterReachingFailureThreshold() {
        OllamaCircuitBreaker breaker = newBreaker();

        breaker.onFailure(); // 1
        breaker.onFailure(); // 2 -> OPEN

        assertThatThrownBy(breaker::acquire).isInstanceOf(CircuitOpenException.class);
    }

    @Test
    void successResetsFailureCount() {
        OllamaCircuitBreaker breaker = newBreaker();

        breaker.onFailure();  // 1
        breaker.onSuccess();  // reset
        breaker.onFailure();  // 1 again -> still below threshold

        assertThatCode(breaker::acquire).doesNotThrowAnyException();
    }

    @Test
    void movesToHalfOpenAfterWaitAndClosesOnSuccess() throws InterruptedException {
        OllamaCircuitBreaker breaker = newBreaker();
        breaker.onFailure();
        breaker.onFailure(); // OPEN

        Thread.sleep(OPEN_WAIT_MS + 50);

        // Cool-down elapsed: a trial call is allowed (HALF_OPEN), and success closes it.
        assertThatCode(breaker::acquire).doesNotThrowAnyException();
        breaker.onSuccess();
        assertThatCode(breaker::acquire).doesNotThrowAnyException();
    }

    @Test
    void reopensWhenHalfOpenTrialFails() throws InterruptedException {
        OllamaCircuitBreaker breaker = newBreaker();
        breaker.onFailure();
        breaker.onFailure(); // OPEN

        Thread.sleep(OPEN_WAIT_MS + 50);

        breaker.acquire();    // HALF_OPEN trial permitted
        breaker.onFailure();  // trial fails -> OPEN again immediately

        assertThatThrownBy(breaker::acquire).isInstanceOf(CircuitOpenException.class);
    }
}
