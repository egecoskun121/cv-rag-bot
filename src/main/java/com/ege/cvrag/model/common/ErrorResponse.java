package com.ege.cvrag.model.common;

/** Uniform error payload returned by the global exception handler. */
public record ErrorResponse(int status, String message) {}
