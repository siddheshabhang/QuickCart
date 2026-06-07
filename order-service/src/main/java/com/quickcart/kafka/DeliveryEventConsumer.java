package com.quickcart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcart.common.event.DeliveryStatusChangedEvent;
import com.quickcart.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventConsumer {
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @KafkaListener(topics = "delivery-events", groupId = "order-delivery-sync-group")
    public void consume(String message) {
        try {
            DeliveryStatusChangedEvent event = objectMapper.readValue(message, DeliveryStatusChangedEvent.class);
            orderService.syncStatusFromDelivery(event);
        } catch (Exception e) {
            log.error("Failed to consume delivery event: {}", e.getMessage(), e);
        }
    }
}
