package com.ege.cvrag.kafka;

import com.ege.cvrag.ingestion.MarkdownIndexer;
import com.ege.cvrag.model.ingestion.IngestRequestEvent;
import com.ege.cvrag.vectorstore.PgVectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * End-to-end event flow over an in-JVM Kafka broker (no Docker). Boots the app in
 * {@code kafka} mode with the vector store and indexer mocked, publishes an ingest
 * request, and asserts the consumer picks it up and indexes the source. Proves the
 * producer → topic → consumer wiring (serialization, listener, source lookup).
 */
@SpringBootTest(properties = {
        "app.ingestion.mode=kafka",
        "app.github.enabled=false",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = {"document-ingest-requests", "document-ingest-requests.DLT"})
class IngestionKafkaFlowIntegrationTest {

    @MockBean
    private PgVectorStore vectorStore;   // no real Postgres in CI

    @MockBean
    private MarkdownIndexer indexer;     // control indexing without embeddings

    @Autowired
    private IngestionEventPublisher publisher;

    @Test
    void publishedEventIsConsumedAndIndexed() throws InterruptedException {
        CountDownLatch indexed = new CountDownLatch(1);
        when(indexer.index(anyString())).thenAnswer(invocation -> {
            indexed.countDown();
            return 14;
        });

        publisher.publish(new IngestRequestEvent("CV (cv.md)"));

        assertThat(indexed.await(20, TimeUnit.SECONDS))
                .as("consumer should index the source published to Kafka")
                .isTrue();
    }
}
