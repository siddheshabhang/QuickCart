package com.quickcart.service;

import com.quickcart.common.event.DeliveryStatusChangedEvent;
import com.quickcart.common.exception.ResourceNotFoundException;
import com.quickcart.entiry.Delivery;
import com.quickcart.entiry.DeliveryStatus;
import com.quickcart.kafka.DeliveryProducer;
import com.quickcart.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final DeliveryProducer deliveryProducer;
    private final OtpService otpService;

    @Transactional
    public void createDelivery(Long orderId, Long userId) {
        if (deliveryRepository.findByOrderId(orderId).isPresent()) {
            log.warn("Delivery already exists for orderId: {}", orderId);
            return;
        }
        Delivery delivery = Delivery.builder()
                .orderId(orderId)
                .userId(userId)
                .status(DeliveryStatus.ASSIGNED)
                .build();
        deliveryRepository.save(delivery);

        deliveryProducer.publishDeliveryStatus(DeliveryStatusChangedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .status(DeliveryStatus.ASSIGNED.name())
                .build());
    }

    @Transactional
    public void updateStatus(Long orderId, DeliveryStatus newStatus) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for orderId: " + orderId));

        if (newStatus == DeliveryStatus.OUT_FOR_DELIVERY) {
            String otp = otpService.generateOtp(orderId);
            log.info("OTP for orderId {}: {}", orderId, otp);
        }

        delivery.setStatus(newStatus);
        deliveryRepository.save(delivery);

        deliveryProducer.publishDeliveryStatus(DeliveryStatusChangedEvent.builder()
                .orderId(orderId)
                .userId(delivery.getUserId())
                .status(newStatus.name())
                .build());
    }

    @Transactional
    public void verifyOtp(Long orderId, String otp) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for orderId: " + orderId));

        if (delivery.getStatus() != DeliveryStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException("OTP can only be verified when OUT_FOR_DELIVERY");
        }

        if (!otpService.verifyOtp(orderId, otp)) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        delivery.setStatus(DeliveryStatus.DELIVERED);
        deliveryRepository.save(delivery);

        deliveryProducer.publishDeliveryStatus(DeliveryStatusChangedEvent.builder()
                .orderId(orderId)
                .userId(delivery.getUserId())
                .status(DeliveryStatus.DELIVERED.name())
                .build());
    }
}