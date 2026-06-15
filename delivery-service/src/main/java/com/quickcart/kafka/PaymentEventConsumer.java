package com.quickcart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcart.common.event.PaymentCompletedEvent;
import com.quickcart.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {
    private final ObjectMapper objectMapper;
    private final DeliveryService deliveryService;

    @KafkaListener(topics = "payment-events", groupId = "delivery-group")
    public void consume(String message) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
            if (event.getStatus().equals("SUCCESS")) {
                deliveryService.createDelivery(event.getOrderId(), event.getUserId(), event.getUserEmail());
                log.info("Delivery created for orderId: {}", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to consume payment event: {}", e.getMessage());
        }
    }
}