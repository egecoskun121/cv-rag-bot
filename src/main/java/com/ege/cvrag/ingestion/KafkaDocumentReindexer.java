package com.ege.cvrag.ingestion;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.kafka.IngestionEventPublisher;
import com.ege.cvrag.model.ingestion.IngestRequestEvent;
import com.ege.cvrag.model.ingestion.IngestionSummary;
import com.ege.cvrag.model.ingestion.SourceIngestResult;
import com.ege.cvrag.startup.RunOnStartup;
import com.ege.cvrag.vectorstore.PgVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Event-driven reindexer: clears the store, then publishes one
 * {@link IngestRequestEvent} per source instead of indexing inline. The actual
 * embed + store happens in {@link com.ege.cvrag.kafka.IngestionEventConsumer},
 * asynchronously and with retry + dead-letter handling. Active when
 * {@code app.ingestion.mode=kafka}.
 *
 * The returned summary reports each source as {@code queued} (0 chunks) — the real
 * counts are logged by the consumer as events are processed.
 */
@Component
@ConditionalOnProperty(prefix = "app.ingestion", name = "mode",
        havingValue = RagBotConstants.INGEST_MODE_KAFKA)
public class KafkaDocumentReindexer implements DocumentReindexer {

    private static final Logger log = LoggerFactory.getLogger(KafkaDocumentReindexer.class);

    private final List<DocumentSource> sources;
    private final PgVectorStore vectorStore;
    private final IngestionEventPublisher publisher;
    private final boolean reloadOnStartup;

    public KafkaDocumentReindexer(List<DocumentSource> sources,
                                  PgVectorStore vectorStore,
                                  IngestionEventPublisher publisher,
                                  @Value("${app.ingestion.reload-on-startup:true}") boolean reloadOnStartup) {
        this.sources = sources;
        this.vectorStore = vectorStore;
        this.publisher = publisher;
        this.reloadOnStartup = reloadOnStartup;
    }

    @Override
    @RunOnStartup
    public IngestionSummary reindex() {
        if (reloadOnStartup) {
            int deleted = vectorStore.deleteAll();
            log.info("Cleared {} existing vector rows before re-ingestion", deleted);
        }
        List<SourceIngestResult> results = sources.stream().map(this::queue).toList();
        log.info("Queued {} source(s) for async ingestion", results.size());
        return new IngestionSummary(0, results);
    }

    private SourceIngestResult queue(DocumentSource source) {
        publisher.publish(new IngestRequestEvent(source.name()));
        return new SourceIngestResult(source.name(), 0, RagBotConstants.INGEST_STATUS_QUEUED);
    }
}
