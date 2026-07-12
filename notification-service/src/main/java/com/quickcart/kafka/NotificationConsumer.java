package com.quickcart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcart.common.event.DeliveryStatusChangedEvent;
import com.quickcart.common.event.PaymentCompletedEvent;
import com.quickcart.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that reacts to payment and delivery events
 * by sending real transactional emails to the customer.
 *
 * <p>Each listener method:
 * <ol>
 *   <li>Deserialises the JSON message into the matching event class.</li>
 *   <li>Delegates to {@link EmailService} to send the appropriate email.</li>
 *   <li>Catches all exceptions — a Kafka listener must NEVER throw, or the
 *       message will be re-delivered in an infinite loop.</li>
 * </ol>
 *
 * <p>The {@code userEmail} field on each event is populated by the originating
 * service (payment-service, delivery-service) by reading
 * {@code SecurityContextHolder.getContext().getAuthentication().getCredentials()},
 * which holds the value of the {@code X-User-Email} header forwarded by the
 * API Gateway after JWT validation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @KafkaListener(topics = "payment-events", groupId = "notification-group")
    public void consumePaymentEvent(String message) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
            log.info("Payment event received → orderId: {}, status: {}, email: {}",
                    event.getOrderId(), event.getStatus(), event.getUserEmail());

            if ("SUCCESS".equals(event.getStatus())) {
                emailService.sendPaymentSuccess(event.getUserEmail(), event.getOrderId(), event.getAmount());
            } else {
                emailService.sendPaymentFailed(event.getUserEmail(), event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "delivery-events", groupId = "notification-group")
    public void consumeDeliveryEvent(String message) {
        try {
            DeliveryStatusChangedEvent event = objectMapper.readValue(message, DeliveryStatusChangedEvent.class);
            log.info("Delivery event received → orderId: {}, status: {}, email: {}",
                    event.getOrderId(), event.getStatus(), event.getUserEmail());
            emailService.sendDeliveryUpdate(event.getUserEmail(), event.getOrderId(), event.getStatus());
        } catch (Exception e) {
            log.error("Failed to process delivery event: {}", e.getMessage(), e);
        }
    }
}