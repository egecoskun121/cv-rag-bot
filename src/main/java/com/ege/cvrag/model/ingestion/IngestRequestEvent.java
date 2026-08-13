package com.ege.cvrag.model.ingestion;

/**
 * A request to (re)index one document source, carried over Kafka. It holds only
 * the source <em>name</em> — not the content — so the consumer fetches fresh
 * Markdown at processing time. That means a transient fetch failure (e.g. a
 * GitHub rate limit) is retried against live data rather than a stale snapshot.
 */
public record IngestRequestEvent(String source) {}
