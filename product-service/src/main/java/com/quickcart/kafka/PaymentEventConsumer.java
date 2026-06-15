package com.quickcart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcart.common.event.PaymentCompletedEvent;
import com.quickcart.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to the {@code payment-events} topic and reacts
 * to payment outcomes by confirming or releasing the matching stock reservation.
 *
 * <p>Topic:          {@code payment-events}<br>
 * Group ID:        {@code product-service-group}<br>
 * Message format:  JSON representation of {@link PaymentCompletedEvent}
 *
 * <p><b>Why product-service consumes payment events?</b><br>
 * When an order is placed, product-service soft-locks the stock (RESERVED).
 * Whether to permanently deduct (CONFIRMED) or return the stock (RELEASED)
 * depends on whether payment succeeded or failed. Instead of a synchronous
 * Feign call from payment-service back to product-service (which would create
 * a circular dependency), we use an asynchronous Kafka event. This keeps the
 * two services decoupled and resilient to partial failures.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final StockReservationService stockReservationService;

    /**
     * Receives every message from {@code payment-events} and routes it to
     * the appropriate reservation lifecycle method.
     *
     * @param message raw JSON string published by payment-service
     */
    @KafkaListener(
            topics = "payment-events",
            groupId = "product-service-group"
    )
    public void onPaymentEvent(String message) {
        log.info("PaymentEventConsumer received message: {}", message);
        try {
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
            handlePaymentEvent(event);
        } catch (Exception e) {
            log.error("Failed to process payment-event message: {}", message, e);
            // In production, dead-letter to a separate topic or alert ops.
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void handlePaymentEvent(PaymentCompletedEvent event) {
        Long orderId = event.getOrderId();
        String status = event.getStatus();

        log.info("Processing payment event → orderId: {}, status: {}", orderId, status);

        switch (status) {
            case "SUCCESS" -> {
                // Payment went through: permanently keep the deducted stock
                stockReservationService.confirmReservation(orderId);
                log.info("Reservation CONFIRMED for orderId: {}", orderId);
            }
            case "FAILED" -> {
                // Payment failed: give the stock back
                stockReservationService.releaseReservation(orderId);
                log.info("Reservation RELEASED for orderId: {}", orderId);
            }
            default -> log.warn("Unknown payment status '{}' for orderId: {} — ignoring", status, orderId);
        }
    }
}
