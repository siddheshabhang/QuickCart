package com.quickcart.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka-related configuration for product-service.
 *
 * <p>An {@link ObjectMapper} bean is declared here so that the
 * {@link com.quickcart.kafka.PaymentEventConsumer} can deserialise incoming
 * JSON messages that contain Java 8+ date/time types (e.g. {@code LocalDateTime}).
 * Without registering {@code JavaTimeModule}, Jackson throws
 * {@code InvalidDefinitionException} on those fields.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Support Java 8 date/time types (LocalDateTime, Instant, etc.)
        mapper.registerModule(new JavaTimeModule());
        // Serialise dates as ISO-8601 strings, not millisecond timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
