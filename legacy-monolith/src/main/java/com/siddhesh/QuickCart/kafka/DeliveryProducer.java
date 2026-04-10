package com.siddhesh.QuickCart.kafka;

import com.siddhesh.QuickCart.kafka.event.DeliveryStatusChangedEvent;
import com.siddhesh.QuickCart.kafka.event.OrderAssignedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class DeliveryProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "delivery-events";

    public void publishOrderAssignedEvent(OrderAssignedEvent event) {
        try {
            String msg = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, msg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize OrderAssignedEvent", e);
        }
    }

    public void publishDeliveryStatusEvent(DeliveryStatusChangedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize DeliveryStatusChangedEvent", e);
        }
    }
}
