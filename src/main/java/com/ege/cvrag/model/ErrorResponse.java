package com.ege.cvrag.model;

/** Uniform error payload returned by the global exception handler. */
public record ErrorResponse(int status, String message) {}
