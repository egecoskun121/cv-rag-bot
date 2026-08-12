package com.ege.cvrag.circuitbreaker;

/**
 * Circuit breaker states.
 *   CLOSED    - calls pass through; failures are counted.
 *   OPEN      - calls are rejected immediately (fail fast) during the cool-down.
 *   HALF_OPEN - a single trial call is allowed to probe recovery.
 */
public enum CircuitState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
