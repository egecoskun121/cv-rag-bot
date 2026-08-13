package com.ege.cvrag.model.ingestion;

import java.util.List;

/**
 * The result of a full re-index: the total chunks stored and the per-source
 * breakdown. Returned from {@code POST /api/v1/reindex} so a caller can see what
 * was (re)loaded without reading the logs.
 */
public record IngestionSummary(int totalChunks, List<SourceIngestResult> sources) {}
