package com.ege.cvrag.ingestion;

import com.ege.cvrag.model.ingestion.IngestionSummary;

/**
 * Strategy for (re)indexing every {@link DocumentSource} into the vector store.
 * Two implementations are wired by {@code app.ingestion.mode}: {@code sync} does
 * the work inline; {@code kafka} publishes one event per source and lets a
 * consumer index them asynchronously. Callers (the startup hook and
 * {@code POST /api/v1/reindex}) depend only on this interface.
 */
public interface DocumentReindexer {

    /** Re-index all sources and return the per-source outcome. */
    IngestionSummary reindex();
}
