package com.quickcart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcart.common.event.OrderCreatedEvent;
import com.quickcart.common.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "notification-group")
    public void consumeOrderEvent(String message) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            log.info("📦 Order placed notification → orderId: {}, userId: {}, amount: {}",
                    event.getOrderId(), event.getUserId(), event.getTotalAmount());
        } catch (Exception e) {
            log.error("Failed to consume order event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-group")
    public void consumePaymentEvent(String message) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
            if (event.getStatus().equals("SUCCESS")) {
                log.info("✅ Payment success notification → orderId: {}, amount: {}",
                        event.getOrderId(), event.getAmount());
            } else {
                log.info("❌ Payment failed notification → orderId: {}",
                        event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to consume payment event: {}", e.getMessage());
        }
    }
}