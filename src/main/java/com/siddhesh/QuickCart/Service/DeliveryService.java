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

    public void updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

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
        if (current == OrderStatus.OUT_FOR_DELIVERY && next == OrderStatus.DELIVERED) return;
        throw new RuntimeException("Invalid status transition");
    }
}
