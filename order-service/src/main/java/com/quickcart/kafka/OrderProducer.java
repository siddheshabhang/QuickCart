package com.quickcart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcart.common.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "order-events";

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            String msg = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, msg);
            log.info("Order event published → orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish order event for orderId: {}", event.getOrderId(), e);
        }
    }
}
