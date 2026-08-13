package com.ege.cvrag.model.ingestion;

/**
 * The outcome of indexing a single {@link com.ege.cvrag.ingestion.DocumentSource}:
 * its name, how many chunks were stored, and a human-readable status
 * ({@code indexed} or {@code skipped: <reason>}).
 */
public record SourceIngestResult(String source, int chunks, String status) {}
