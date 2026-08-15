package com.ege.cvrag.kafka;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.qa.QaEvent;
import io.apicurio.registry.serde.config.SerdeConfig;
import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaDeserializer;
import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Dedicated producer/consumer for {@link QaEvent}, using Apicurio's JSON Schema
 * serde instead of the plain JSON one the rest of the app uses. Kept separate from
 * the global Kafka config (which stays plain JSON for {@code IngestRequestEvent})
 * because {@code @KafkaListener}/{@code KafkaTemplate} serialization is configured
 * per producer/consumer factory, not per message type.
 *
 * The serializer registers (and validates against) a schema in Apicurio the first
 * time it publishes; from then on, an incompatible change to {@link QaEvent} is
 * rejected at publish time instead of silently breaking a consumer later.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.qa", name = "events.enabled", havingValue = "true")
public class QaEventSchemaConfig {

    @Bean
    public KafkaTemplate<String, QaEvent> qaEventKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${app.qa.schema-registry-url}") String registryUrl) {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSchemaKafkaSerializer.class);
        props.put(SerdeConfig.REGISTRY_URL, registryUrl);
        props.put(SerdeConfig.AUTO_REGISTER_ARTIFACT, true);
        props.put(SerdeConfig.EXPLICIT_ARTIFACT_GROUP_ID, RagBotConstants.QA_EVENT_SCHEMA_GROUP);
        props.put(SerdeConfig.EXPLICIT_ARTIFACT_ID, RagBotConstants.QA_EVENT_SCHEMA_ARTIFACT_ID);

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QaEvent> qaEventListenerContainerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${app.qa.schema-registry-url}") String registryUrl) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, RagBotConstants.QA_EVENTS_CONSUMER_GROUP);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSchemaKafkaDeserializer.class);
        props.put(SerdeConfig.REGISTRY_URL, registryUrl);
        props.put(SerdeConfig.DESERIALIZER_SPECIFIC_VALUE_RETURN_CLASS, QaEvent.class);

        ConcurrentKafkaListenerContainerFactory<String, QaEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        return factory;
    }
}
