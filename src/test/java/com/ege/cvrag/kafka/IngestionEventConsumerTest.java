package com.ege.cvrag.kafka;

import com.ege.cvrag.ingestion.DocumentSource;
import com.ege.cvrag.ingestion.MarkdownIndexer;
import com.ege.cvrag.model.ingestion.IngestRequestEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the consumer's own logic — no broker. They pin the two behaviours
 * the dead-letter flow depends on: an unknown source is dropped (not retried), and
 * an indexing failure propagates (so the container routes it to the DLT).
 */
class IngestionEventConsumerTest {

    private record FixedSource(String name, String markdown) implements DocumentSource {}

    @Test
    void indexesTheNamedSource() {
        MarkdownIndexer indexer = mock(MarkdownIndexer.class);
        when(indexer.index("cv-text")).thenReturn(14);
        IngestionEventConsumer consumer = new IngestionEventConsumer(
                List.of(new FixedSource("CV", "cv-text")), indexer);

        consumer.onIngestRequest(new IngestRequestEvent("CV"));

        verify(indexer).index("cv-text");
    }

    @Test
    void dropsUnknownSourceWithoutIndexing() {
        MarkdownIndexer indexer = mock(MarkdownIndexer.class);
        IngestionEventConsumer consumer = new IngestionEventConsumer(
                List.of(new FixedSource("CV", "cv-text")), indexer);

        consumer.onIngestRequest(new IngestRequestEvent("Ghost"));

        verify(indexer, never()).index(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void propagatesIndexingFailureSoItReachesTheDeadLetterTopic() {
        MarkdownIndexer indexer = mock(MarkdownIndexer.class);
        when(indexer.index("cv-text")).thenThrow(new IllegalStateException("embed failed"));
        IngestionEventConsumer consumer = new IngestionEventConsumer(
                List.of(new FixedSource("CV", "cv-text")), indexer);

        assertThatThrownBy(() -> consumer.onIngestRequest(new IngestRequestEvent("CV")))
                .isInstanceOf(IllegalStateException.class);
    }
}
