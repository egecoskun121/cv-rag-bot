package com.ege.cvrag.kafka;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.ingestion.IngestRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link IngestRequestEvent}s to the ingestion-requests topic. Keyed by
 * source name so all requests for the same source land on one partition and are
 * processed in order. Only wired in when {@code app.ingestion.mode=kafka}.
 */
@Component
@ConditionalOnProperty(prefix = "app.ingestion", name = "mode",
        havingValue = RagBotConstants.INGEST_MODE_KAFKA)
public class IngestionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(IngestionEventPublisher.class);

    private final KafkaTemplate<String, IngestRequestEvent> kafkaTemplate;
    private final String requestsTopic;

    public IngestionEventPublisher(KafkaTemplate<String, IngestRequestEvent> kafkaTemplate,
                                   @Value("${app.ingestion.kafka.requests-topic}") String requestsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.requestsTopic = requestsTopic;
    }

    public void publish(IngestRequestEvent event) {
        log.info("Publishing ingest request for source {}", event.source());
        kafkaTemplate.send(requestsTopic, event.source(), event);
    }
}
