package com.ege.cvrag.kafka;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.ingestion.IngestRequestEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka wiring for the event-driven ingestion pipeline (only active when
 * {@code app.ingestion.mode=kafka}): declares the request topic and its
 * dead-letter topic, and installs an error handler that retries a failed record a
 * few times (fixed backoff) before publishing it to {@code <topic>.DLT}.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.ingestion", name = "mode",
        havingValue = RagBotConstants.INGEST_MODE_KAFKA)
public class KafkaIngestionConfig {

    private final String requestsTopic;
    private final int retryAttempts;
    private final long retryBackoffMs;

    public KafkaIngestionConfig(@Value("${app.ingestion.kafka.requests-topic}") String requestsTopic,
                                @Value("${app.ingestion.kafka.retry-attempts:3}") int retryAttempts,
                                @Value("${app.ingestion.kafka.retry-backoff-ms:1000}") long retryBackoffMs) {
        this.requestsTopic = requestsTopic;
        this.retryAttempts = retryAttempts;
        this.retryBackoffMs = retryBackoffMs;
    }

    @Bean
    public NewTopic ingestRequestsTopic() {
        return TopicBuilder.name(requestsTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic ingestRequestsDlt() {
        return TopicBuilder.name(requestsTopic + ".DLT").partitions(1).replicas(1).build();
    }

    /**
     * Retries a failed record {@code retryAttempts} times, then routes it to
     * {@code <topic>.DLT}. Boot picks this up as the listener error handler.
     */
    @Bean
    public DefaultErrorHandler ingestErrorHandler(KafkaTemplate<String, IngestRequestEvent> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(retryBackoffMs, retryAttempts));
    }
}
