package com.siddhesh.QuickCart.Service;

import com.siddhesh.QuickCart.Entity.Order;
import com.siddhesh.QuickCart.Entity.OrderStatus;
import com.siddhesh.QuickCart.Exception.ResourceNotFoundException;
import com.siddhesh.QuickCart.Repository.OrderRepository;
import com.siddhesh.QuickCart.kafka.DeliveryProducer;
import com.siddhesh.QuickCart.kafka.event.DeliveryStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeliveryService {
    private final OrderRepository orderRepository;
    private final DeliveryProducer deliveryProducer;
    private final OtpService otpService;

    public void updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (newStatus == OrderStatus.OUT_FOR_DELIVERY) {
            otpService.generateOtp(orderId);
        }

        validateTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        orderRepository.save(order);

        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(
                order.getId(),
                order.getUser().getId(),
                order.getStatus()
        );
        deliveryProducer.publishDeliveryStatusEvent(event);
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        if (current == OrderStatus.ASSIGNED && next == OrderStatus.OUT_FOR_DELIVERY) return;
        throw new RuntimeException("Invalid status transition");
    }

    public void verifyOtp(Long orderId, String otp) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new RuntimeException("OTP can only be verified in OUT_FOR_DELIVERY state");
        }

        boolean isValid = otpService.verifyOtp(orderId, otp);
        if (!isValid) {
            throw new RuntimeException("Invalid OTP");
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(
                order.getId(),
                order.getUser().getId(),
                OrderStatus.DELIVERED);
        deliveryProducer.publishDeliveryStatusEvent(event);
    }
}
