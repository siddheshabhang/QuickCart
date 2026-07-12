package com.quickcart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Central email service for notification-service.
 *
 * <p>Sends transactional emails for every key event in the QuickCart order lifecycle:
 * <ul>
 *   <li>Payment confirmed (serves as the primary order confirmation) / payment failed</li>
 *   <li>Delivery status updates (assigned, out for delivery, delivered, failed)</li>
 * </ul>
 *
 * <p>The OTP email is intentionally NOT handled here — it is sent by delivery-service
 * directly, so that it can be triggered immediately on status change without an
 * extra Kafka round-trip.
 *
 * <p>All send failures are caught and logged, not propagated. A Kafka consumer
 * must not throw on email failure — doing so would reprocess the message endlessly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    // ─────────────────────────────────────────────────────────────────────────
    // Payment emails
    // ─────────────────────────────────────────────────────────────────────────

    public void sendPaymentSuccess(String to, Long orderId, Double amount) {
        send(to,
                "QuickCart — Payment Confirmed ✅ for Order #" + orderId,
                "Great news!\n\n"
                + "Your payment of ₹" + String.format("%.2f", amount) + " was successful.\n"
                + "Order #" + orderId + " is now being prepared for delivery.\n\n"
                + "You'll receive another update once a delivery agent is assigned.\n\n"
                + "Thank you for shopping with QuickCart!");
    }

    public void sendPaymentFailed(String to, Long orderId) {
        send(to,
                "QuickCart — Payment Failed ❌ for Order #" + orderId,
                "Unfortunately, your payment for Order #" + orderId + " could not be processed.\n\n"
                + "Please try again with a different payment method.\n\n"
                + "If the amount was deducted from your account, it will be refunded within 5-7 business days.\n\n"
                + "QuickCart Support");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delivery emails
    // ─────────────────────────────────────────────────────────────────────────

    public void sendDeliveryUpdate(String to, Long orderId, String status) {
        String subject = "QuickCart — Order #" + orderId + " Update: " + status;
        String body = switch (status) {
            case "ASSIGNED" ->
                    "A delivery agent has been assigned to your order #" + orderId + ".\n"
                    + "Your order will be picked up from our store and dispatched shortly.";
            case "OUT_FOR_DELIVERY" ->
                    "Your order #" + orderId + " is out for delivery! 🛵\n"
                    + "Check your inbox for a separate email with the delivery OTP.\n"
                    + "Please share the OTP with the delivery agent to confirm receipt.";
            case "DELIVERED" ->
                    "Your order #" + orderId + " has been delivered successfully! 🎉\n"
                    + "We hope you enjoy your purchase. Thank you for choosing QuickCart!";
            case "FAILED" ->
                    "We're sorry — delivery of order #" + orderId + " failed.\n"
                    + "Our support team will contact you shortly to arrange a re-delivery.";
            default ->
                    "Your order #" + orderId + " status has been updated to: " + status;
        };
        send(to, subject, body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helper
    // ─────────────────────────────────────────────────────────────────────────

    private void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Skipping email — recipient address is null or blank. subject: {}", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent → to: {}, subject: {}", to, subject);
        } catch (Exception e) {
            // Log and swallow — never let an email failure break Kafka message processing
            log.error("Failed to send email → to: {}, subject: {}", to, subject, e);
        }
    }
}
