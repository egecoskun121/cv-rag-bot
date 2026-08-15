package com.ege.cvrag.kafka;

import com.ege.cvrag.model.qa.QaEvent;
import com.ege.cvrag.qa.QaStatsAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link QaEvent}s and folds them into {@link QaStatsAggregator}. Kept off
 * the request path on purpose: answering a question just publishes; all the
 * analytics work happens here, asynchronously. A weak-retrieval event is logged so
 * it's visible even without polling the stats endpoint.
 */
@Component
@ConditionalOnProperty(prefix = "app.qa", name = "events.enabled", havingValue = "true")
public class QaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(QaEventConsumer.class);

    private final QaStatsAggregator stats;

    public QaEventConsumer(QaStatsAggregator stats) {
        this.stats = stats;
    }

    @KafkaListener(topics = "${app.qa.topic}", containerFactory = "qaEventListenerContainerFactory")
    public void onQaEvent(QaEvent event) {
        stats.record(event);
        if (stats.isWeak(event.topScore())) {
            log.warn("Weak retrieval (topScore={}) for question: {}", event.topScore(), event.question());
        }
    }
}
