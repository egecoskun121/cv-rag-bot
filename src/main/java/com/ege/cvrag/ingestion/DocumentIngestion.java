package com.ege.cvrag.ingestion;

import com.ege.cvrag.startup.RunOnStartup;
import com.ege.cvrag.vectorstore.PgVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Indexes every {@link DocumentSource} into the vector store at startup.
 *
 * Spring injects all {@code DocumentSource} beans (ordered by {@code @Order}), so
 * adding a source never changes this class. The reload-clear happens once, up
 * front; each source is indexed independently, and a failing source is logged
 * and skipped so the rest still make it in.
 */
@Component
public class DocumentIngestion {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestion.class);

    private final List<DocumentSource> sources;
    private final MarkdownIndexer indexer;
    private final PgVectorStore vectorStore;
    private final boolean reloadOnStartup;

    public DocumentIngestion(List<DocumentSource> sources,
                             MarkdownIndexer indexer,
                             PgVectorStore vectorStore,
                             @Value("${app.ingestion.reload-on-startup:true}") boolean reloadOnStartup) {
        this.sources = sources;
        this.indexer = indexer;
        this.vectorStore = vectorStore;
        this.reloadOnStartup = reloadOnStartup;
    }

    @RunOnStartup
    public void ingest() {
        if (reloadOnStartup) {
            int deleted = vectorStore.deleteAll();
            log.info("Cleared {} existing vector rows before re-ingestion", deleted);
        }
        sources.forEach(this::indexSource);
    }

    private void indexSource(DocumentSource source) {
        try {
            int count = indexer.index(source.markdown());
            log.info("Indexed {} chunks from {}", count, source.name());
        } catch (RuntimeException ex) {
            log.warn("Skipping source {} — {}", source.name(), ex.getMessage());
        }
    }
}
