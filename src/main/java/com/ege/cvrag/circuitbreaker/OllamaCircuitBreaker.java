package com.ege.cvrag.circuitbreaker;

import com.ege.cvrag.constant.RagBotConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A minimal circuit breaker for Ollama calls.
 *
 * States:
 *   CLOSED    - calls pass through; consecutive failures are counted.
 *   OPEN      - calls are rejected immediately (fail fast) for a cool-down period.
 *   HALF_OPEN - after the cool-down, a single trial call is allowed; success
 *               closes the circuit, failure re-opens it.
 *
 * Where retry handles a brief blip, the breaker handles a sustained outage: once
 * the dependency looks down, it stops hammering it and fails fast until it has
 * had time to recover.
 *
 * State is guarded by intrinsic locking — enough for a single-instance service.
 */
@Component
public class OllamaCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(OllamaCircuitBreaker.class);

    private enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final long openWaitMs;

    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private long openedAtMs = 0L;

    public OllamaCircuitBreaker(@Value("${app.ollama.circuit-breaker.failure-threshold}") int failureThreshold,
                                @Value("${app.ollama.circuit-breaker.open-wait-ms}") long openWaitMs) {
        this.failureThreshold = failureThreshold;
        this.openWaitMs = openWaitMs;
    }

    /** Call before performing the guarded operation. Throws if the circuit is OPEN. */
    public synchronized void acquire() {
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - openedAtMs >= openWaitMs) {
                state = State.HALF_OPEN;
                log.info("Circuit HALF_OPEN — allowing a trial call");
            } else {
                throw new CircuitOpenException(RagBotConstants.ERROR_AI_CIRCUIT_OPEN);
            }
        }
    }

    /** Report a successful call — closes the circuit and resets the counter. */
    public synchronized void onSuccess() {
        if (state != State.CLOSED) {
            log.info("Circuit CLOSED — dependency healthy again");
        }
        state = State.CLOSED;
        consecutiveFailures = 0;
    }

    /** Report a failed call — may open (or re-open) the circuit. */
    public synchronized void onFailure() {
        consecutiveFailures++;
        if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
            state = State.OPEN;
            openedAtMs = System.currentTimeMillis();
            log.warn("Circuit OPEN after {} consecutive failures — failing fast for {} ms",
                    consecutiveFailures, openWaitMs);
        }
    }
}
