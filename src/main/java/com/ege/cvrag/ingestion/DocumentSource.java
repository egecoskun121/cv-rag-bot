package com.ege.cvrag.ingestion;

/**
 * A source of Markdown to index into the vector store. Every implementation is a
 * Spring {@code @Component}; the ingestion orchestrator gets them all injected as
 * a {@code List<DocumentSource>}, so adding a source (CV, GitHub, S3, …) never
 * touches the orchestrator — just add a new component (Open/Closed).
 */
public interface DocumentSource {

    /** Human-readable name for logging. */
    String name();

    /** The Markdown to index (chunked section-by-section by the indexer). */
    String markdown();
}
