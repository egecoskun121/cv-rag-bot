package com.ege.cvrag.circuitbreaker;

/** Thrown when a call is rejected because the circuit is OPEN. */
public class CircuitOpenException extends RuntimeException {

    public CircuitOpenException(String message) {
        super(message);
    }
}
