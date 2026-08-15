package com.ege.cvrag.kafka;

import com.ege.cvrag.model.qa.QaEvent;
import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaDeserializer;
import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the producer/consumer factories are wired with Apicurio's JSON Schema
 * serde. Building them doesn't connect to Kafka or the registry (both are lazy),
 * so this is CI-safe — no Docker needed.
 */
class QaEventSchemaConfigTest {

    private final QaEventSchemaConfig config = new QaEventSchemaConfig();

    @Test
    void producerUsesJsonSchemaSerializer() {
        KafkaTemplate<String, QaEvent> template = config.qaEventKafkaTemplate("localhost:9092", "http://localhost:8085/apis/registry/v3");

        Object serializer = template.getProducerFactory().getConfigurationProperties()
                .get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);
        assertThat(serializer).isEqualTo(JsonSchemaKafkaSerializer.class);
    }

    @Test
    void consumerFactoryUsesJsonSchemaDeserializer() {
        ConcurrentKafkaListenerContainerFactory<String, QaEvent> factory =
                config.qaEventListenerContainerFactory("localhost:9092", "http://localhost:8085/apis/registry/v3");

        assertThat(factory.getConsumerFactory().getConfigurationProperties())
                .containsEntry("value.deserializer", JsonSchemaKafkaDeserializer.class);
    }
}
