package com.ege.cvrag.kafka;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.ingestion.DocumentSource;
import com.ege.cvrag.ingestion.MarkdownIndexer;
import com.ege.cvrag.model.ingestion.IngestRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consumes {@link IngestRequestEvent}s and indexes the named source. Fetching the
 * Markdown here (not in the producer) means a transient failure is retried against
 * live data. Any exception propagates to the container error handler, which retries
 * a few times and then routes the record to the dead-letter topic
 * ({@code <topic>.DLT}); {@link #onDeadLetter} just logs what got parked there.
 */
@Component
@ConditionalOnProperty(prefix = "app.ingestion", name = "mode",
        havingValue = RagBotConstants.INGEST_MODE_KAFKA)
public class IngestionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(IngestionEventConsumer.class);

    private final Map<String, DocumentSource> sourcesByName;
    private final MarkdownIndexer indexer;

    public IngestionEventConsumer(List<DocumentSource> sources, MarkdownIndexer indexer) {
        this.sourcesByName = sources.stream()
                .collect(Collectors.toMap(DocumentSource::name, Function.identity()));
        this.indexer = indexer;
    }

    @KafkaListener(topics = "${app.ingestion.kafka.requests-topic}")
    public void onIngestRequest(IngestRequestEvent event) {
        DocumentSource source = sourcesByName.get(event.source());
        if (Objects.isNull(source)) {
            log.warn("No source named {} — dropping event", event.source());
            return; // not retryable; a missing source won't appear on a retry
        }
        int count = indexer.index(source.markdown()); // throws on failure -> retry -> DLT
        log.info("Indexed {} chunks from {} (via Kafka)", count, source.name());
    }

    @KafkaListener(topics = "${app.ingestion.kafka.requests-topic}.DLT")
    public void onDeadLetter(IngestRequestEvent event) {
        log.error("Source {} failed all retries — parked in the dead-letter topic", event.source());
    }
}
