package com.ege.cvrag.kafka;

import com.ege.cvrag.ingestion.MarkdownIndexer;
import com.ege.cvrag.model.qa.QaEvent;
import com.ege.cvrag.qa.QaStatsAggregator;
import com.ege.cvrag.vectorstore.PgVectorStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end Q&A event flow. Publishes a {@link QaEvent} and asserts the consumer
 * folds it into {@link QaStatsAggregator}. Since
 * {@link com.ege.cvrag.kafka.QaEventSchemaConfig} routes QaEvent through Apicurio's
 * JSON Schema serde, this now needs a real schema registry reachable (the in-JVM
 * Kafka broker alone isn't enough) — so, like the eval harness, it's off by default
 * and run locally: {@code docker compose up -d kafka apicurio-registry &&
 * mvn test -DschemaRegistry=true -Dtest=QaEventFlowIntegrationTest}
 */
@EnabledIfSystemProperty(named = "schemaRegistry", matches = "true")
@SpringBootTest(properties = {
        "app.qa.events.enabled=true",
        "app.github.enabled=false",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = {"qa-events"})
class QaEventFlowIntegrationTest {

    @MockBean
    private PgVectorStore vectorStore;   // no real Postgres in CI

    @MockBean
    private MarkdownIndexer indexer;     // startup ingestion needs no Ollama

    @Autowired
    private QaEventPublisher publisher;

    @Autowired
    private QaStatsAggregator stats;

    @Test
    void publishedQaEventUpdatesStats() {
        publisher.publish(new QaEvent(
                "Ege hangi dilleri biliyor?", List.of("Skills"), 0.81, "Java, Kotlin", 120, "2026-08-14T00:00:00Z"));

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    assertThat(stats.snapshot().totalQuestions()).isEqualTo(1);
                    assertThat(stats.snapshot().topSections()).containsKey("Skills");
                });
    }
}
