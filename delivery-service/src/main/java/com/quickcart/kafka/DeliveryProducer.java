package com.quickcart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcart.common.event.DeliveryStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "delivery-events";

    public void publishDeliveryStatus(DeliveryStatusChangedEvent event) {
        try {
            String msg = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), msg);
            log.info("Delivery event published → orderId: {}, status: {}",
                    event.getOrderId(), event.getStatus());
        } catch (Exception e) {
            log.error("Failed to publish delivery event for orderId: {}", event.getOrderId(), e);
        }
    }
}
