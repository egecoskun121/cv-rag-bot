package com.ege.cvrag.ingestion;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.ingestion.IngestionSummary;
import com.ege.cvrag.model.ingestion.SourceIngestResult;
import com.ege.cvrag.startup.RunOnStartup;
import com.ege.cvrag.vectorstore.PgVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default (in-process) reindexer: clears the store then indexes each source
 * inline, on the calling thread. A failing source is logged and skipped so the
 * rest still make it in. Active unless {@code app.ingestion.mode=kafka}.
 */
@Component
@ConditionalOnProperty(prefix = "app.ingestion", name = "mode",
        havingValue = RagBotConstants.INGEST_MODE_SYNC, matchIfMissing = true)
public class SyncDocumentReindexer implements DocumentReindexer {

    private static final Logger log = LoggerFactory.getLogger(SyncDocumentReindexer.class);

    private final List<DocumentSource> sources;
    private final MarkdownIndexer indexer;
    private final PgVectorStore vectorStore;
    private final boolean reloadOnStartup;

    public SyncDocumentReindexer(List<DocumentSource> sources,
                                 MarkdownIndexer indexer,
                                 PgVectorStore vectorStore,
                                 @Value("${app.ingestion.reload-on-startup:true}") boolean reloadOnStartup) {
        this.sources = sources;
        this.indexer = indexer;
        this.vectorStore = vectorStore;
        this.reloadOnStartup = reloadOnStartup;
    }

    /**
     * Evicts every cache (GitHub, Medium, cached answers) before reindexing runs.
     * {@code beforeInvocation = true} is required here, not the default: the body
     * below itself calls the {@code @Cacheable} GitHub/Medium clients, so evicting
     * *after* would either (a) immediately wipe the fresh entries this same call
     * just wrote, or worse (b) if a stale entry already existed, the reindex's own
     * "fetch fresh data" call would return that stale cached value instead of
     * hitting the real API — reindexing wouldn't actually refresh anything.
     * Inert unless {@code app.cache.enabled=true} (see {@link com.ege.cvrag.cache.CacheConfig}).
     */
    @Override
    @RunOnStartup
    @CacheEvict(value = {
            RagBotConstants.CACHE_GITHUB_REPOS,
            RagBotConstants.CACHE_GITHUB_LANGUAGES,
            RagBotConstants.CACHE_MEDIUM_FEED,
            RagBotConstants.CACHE_ASK_ANSWERS
    }, allEntries = true, beforeInvocation = true)
    public IngestionSummary reindex() {
        if (reloadOnStartup) {
            int deleted = vectorStore.deleteAll();
            log.info("Cleared {} existing vector rows before re-ingestion", deleted);
        }
        List<SourceIngestResult> results = sources.stream().map(this::indexSource).toList();
        int total = results.stream().mapToInt(SourceIngestResult::chunks).sum();
        log.info("Re-index complete: {} chunks across {} source(s)", total, results.size());
        return new IngestionSummary(total, results);
    }

    private SourceIngestResult indexSource(DocumentSource source) {
        try {
            int count = indexer.index(source.markdown());
            log.info("Indexed {} chunks from {}", count, source.name());
            return new SourceIngestResult(source.name(), count, RagBotConstants.INGEST_STATUS_INDEXED);
        } catch (RuntimeException ex) {
            log.warn("Skipping source {} — {}", source.name(), ex.getMessage());
            return new SourceIngestResult(source.name(), 0,
                    RagBotConstants.INGEST_STATUS_SKIPPED + ex.getMessage());
        }
    }
}
