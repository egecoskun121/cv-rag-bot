package com.ege.cvrag.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Q&amp;A events topic (only when {@code app.qa.events.enabled=true}).
 * Unlike ingestion there's no dead-letter topic here: a malformed analytics event
 * isn't worth retrying, and losing one metric sample is harmless.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.qa", name = "events.enabled", havingValue = "true")
public class KafkaQaConfig {

    @Bean
    public NewTopic qaEventsTopic(@Value("${app.qa.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }
}
