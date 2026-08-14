package com.ege.cvrag.kafka;

import com.ege.cvrag.model.qa.QaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes one {@link QaEvent} per answered question to the Q&amp;A topic. Only
 * wired in when {@code app.qa.events.enabled=true}; when it is absent the RAG path
 * simply doesn't emit events (see {@code RagService}).
 */
@Component
@ConditionalOnProperty(prefix = "app.qa", name = "events.enabled", havingValue = "true")
public class QaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(QaEventPublisher.class);

    private final KafkaTemplate<String, QaEvent> kafkaTemplate;
    private final String topic;

    public QaEventPublisher(KafkaTemplate<String, QaEvent> kafkaTemplate,
                            @Value("${app.qa.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(QaEvent event) {
        log.debug("Publishing Q&A event (topScore={}, latencyMs={})", event.topScore(), event.latencyMs());
        kafkaTemplate.send(topic, event);
    }
}
