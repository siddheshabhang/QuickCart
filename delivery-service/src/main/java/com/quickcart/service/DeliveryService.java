package com.quickcart.service;

import com.quickcart.common.event.DeliveryStatusChangedEvent;
import com.quickcart.common.exception.ResourceNotFoundException;
import com.quickcart.dto.DeliveryResponseDto;
import com.quickcart.entity.Delivery;
import com.quickcart.entity.DeliveryStatus;
import com.quickcart.kafka.DeliveryProducer;
import com.quickcart.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryProducer   deliveryProducer;
    private final OtpService         otpService;
    private final EmailService        emailService;

    // ─────────────────────────────────────────────────────────────────────────
    // Read operations
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DeliveryResponseDto> getAllDeliveries() {
        return deliveryRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeliveryResponseDto getDeliveryByOrderId(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Delivery not found for orderId: " + orderId));
        return toDto(delivery);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Write operations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a delivery record when payment succeeds.
     *
     * <p>The {@code userEmail} comes from the {@code PaymentCompletedEvent} payload —
     * injected by payment-service from the SecurityContext (X-User-Email JWT header).
     * Persisting it here avoids any Feign call when we later need to send the OTP email.
     */
    @Transactional
    public void createDelivery(Long orderId, Long userId, Long storeId, String userEmail) {
        if (deliveryRepository.findByOrderId(orderId).isPresent()) {
            log.warn("Delivery already exists for orderId: {}", orderId);
            return;
        }

        Delivery delivery = Delivery.builder()
                .orderId(orderId)
                .userId(userId)
                .storeId(storeId)
                .userEmail(userEmail)
                .status(DeliveryStatus.ASSIGNED)
                .build();

        deliveryRepository.save(delivery);
        publishStatusEvent(delivery, DeliveryStatus.ASSIGNED);
    }

    /**
     * Updates the delivery status and reacts to the transition.
     *
     * <p>When transitioning to {@code OUT_FOR_DELIVERY}:
     * <ol>
     *   <li>A 6-digit OTP is generated via {@link OtpService} and stored in Redis
     *       with a 5-minute TTL (no DB write required — Redis owns the OTP lifecycle).</li>
     *   <li>The OTP is emailed to the customer via {@link EmailService}.</li>
     *   <li>A {@code log.warn} prints the OTP to console for local dev testing
     *       (remove before production).</li>
     * </ol>
     */
    @Transactional
    public void updateStatus(Long orderId, DeliveryStatus newStatus) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Delivery not found for orderId: " + orderId));

        if (newStatus == DeliveryStatus.OUT_FOR_DELIVERY) {
            String otp = otpService.generateOtp(orderId);

            // DEV ONLY — prints OTP to console so you can test without email configured
            // Remove this line before going to production
            log.warn("DEV ONLY — OTP for orderId {}: {}", orderId, otp);

            emailService.sendOtp(delivery.getUserEmail(), otp, orderId);
        }

        delivery.setStatus(newStatus);
        deliveryRepository.save(delivery);
        publishStatusEvent(delivery, newStatus);
    }

    /**
     * Verifies the OTP entered by the delivery agent and marks the order DELIVERED.
     *
     * <p>Delegates verification entirely to {@link OtpService}, which reads from Redis.
     * The result is an {@link OtpResult} enum — each case maps to a specific exception
     * with a clear user-facing message.
     *
     * <ul>
     *   <li>{@code VALID}                 → status set to DELIVERED, event published</li>
     *   <li>{@code INVALID}               → wrong OTP; attempts counter incremented in Redis</li>
     *   <li>{@code EXPIRED}               → Redis TTL elapsed; agent must request a new OTP</li>
     *   <li>{@code MAX_ATTEMPTS_EXCEEDED} → 3 wrong guesses; OTP invalidated in Redis</li>
     * </ul>
     */
    @Transactional
    public void verifyOtp(Long orderId, String otp) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Delivery not found for orderId: " + orderId));

        if (delivery.getStatus() != DeliveryStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException("OTP can only be verified when status is OUT_FOR_DELIVERY");
        }

        OtpResult result = otpService.verifyOtp(orderId, otp);

        switch (result) {
            case VALID -> {
                delivery.setStatus(DeliveryStatus.DELIVERED);
                deliveryRepository.save(delivery);
                publishStatusEvent(delivery, DeliveryStatus.DELIVERED);
            }
            case INVALID ->
                throw new IllegalArgumentException("Invalid OTP. Please check and try again.");
            case EXPIRED ->
                throw new IllegalStateException("OTP has expired. Please request a new one.");
            case MAX_ATTEMPTS_EXCEEDED ->
                throw new IllegalStateException(
                        "Too many incorrect attempts. OTP has been invalidated. Please request a new one.");
        }
    }

    /**
     * Resends the OTP to the customer if the order is still OUT_FOR_DELIVERY.
     */
    @Transactional
    public void resendOtp(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Delivery not found for orderId: " + orderId));

        if (delivery.getStatus() != DeliveryStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException("OTP can only be resent when status is OUT_FOR_DELIVERY");
        }

        String otp = otpService.generateOtp(orderId);
        log.warn("DEV ONLY — New OTP for orderId {}: {}", orderId, otp);
        emailService.sendOtp(delivery.getUserEmail(), otp, orderId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void publishStatusEvent(Delivery delivery, DeliveryStatus status) {
        deliveryProducer.publishDeliveryStatus(DeliveryStatusChangedEvent.builder()
                .orderId(delivery.getOrderId())
                .userId(delivery.getUserId())
                .status(status.name())
                .userEmail(delivery.getUserEmail())
                .build());
    }

    private DeliveryResponseDto toDto(Delivery delivery) {
        return DeliveryResponseDto.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrderId())
                .userId(delivery.getUserId())
                .storeId(delivery.getStoreId())
                .status(delivery.getStatus())
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}