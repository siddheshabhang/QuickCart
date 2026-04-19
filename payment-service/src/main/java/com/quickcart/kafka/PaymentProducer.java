package com.quickcart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcart.common.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "payment-events";

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            String msg = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, msg);
            log.info("Payment event published → orderId: {}, status: {}",
                    event.getOrderId(), event.getStatus());
        } catch (Exception e) {
            log.error("Failed to publish payment event for orderId: {}", event.getOrderId(), e);
        }
    }
}
