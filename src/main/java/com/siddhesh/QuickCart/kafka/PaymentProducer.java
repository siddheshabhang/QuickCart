package com.siddhesh.QuickCart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siddhesh.QuickCart.kafka.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Create directly — avoids Spring Boot 4.x Jackson 3.x bean conflict
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TOPIC = "payment-events";

    public void publishPaymentEvent(PaymentCompletedEvent event) {
        try {
            String msg = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, msg);
            log.info("Payment event published → orderId: {}, status: {}",
                    event.getOrderId(), event.getStatus());
        } catch (Exception e) {
            log.error("Failed to publish payment event for orderId: {}", event.getOrderId(), e);
            throw new RuntimeException("Failed to serialize payment event", e);
        }
    }
}