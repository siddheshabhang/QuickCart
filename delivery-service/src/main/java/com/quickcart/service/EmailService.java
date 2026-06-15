package com.quickcart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Responsible for sending the delivery OTP email from delivery-service.
 *
 * <p>delivery-service sends only the OTP email. All other transactional emails
 * (order placed, payment confirmed, delivery status updates) are sent by
 * notification-service via Kafka events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    /**
     * Sends the 6-digit OTP to the customer's email when their order goes OUT_FOR_DELIVERY.
     *
     * @param to      customer's email address (stored on Delivery entity)
     * @param otp     the generated OTP
     * @param orderId used to identify the order in the subject line
     */
    public void sendOtp(String to, String otp, Long orderId) {
        if (to == null || to.isBlank()) {
            log.warn("Skipping OTP email — no email address for orderId: {}", orderId);
            return;
        }
        String subject = "QuickCart — Your Delivery OTP for Order #" + orderId;
        String body = "Your delivery OTP is: " + otp
                + "\n\nShare this code with the delivery agent upon arrival to confirm receipt."
                + "\n\n⚠️  Do NOT share this OTP with anyone else."
                + "\n\nThank you for shopping with QuickCart!";
        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent → to: {}, subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email → to: {}, subject: {}", to, subject, e);
        }
    }
}
