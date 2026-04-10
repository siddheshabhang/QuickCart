package com.siddhesh.QuickCart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siddhesh.QuickCart.kafka.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-events", groupId = "notification-group")
    public void consume(String msg) {
        try {
            PaymentCompletedEvent event =
                    objectMapper.readValue(msg, PaymentCompletedEvent.class);
            if (event.getStatus().name().equals("SUCCESS")) {
                System.out.println("📩 Email sent: Payment SUCCESS for Order " + event.getOrderId());
            } else {
                System.out.println("📩 Email sent: Payment FAILED for Order " + event.getOrderId());
            }
        } catch (Exception e) {
            System.out.println("Kafka consume error: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "delivery-events", groupId = "notification-group")
    public void consumeDeliveryEvents(String message) {
        System.out.println("🚚 Delivery Event: " + message);
    }
}
