package com.siddhesh.QuickCart.Service;

import com.siddhesh.QuickCart.Entity.Order;
import com.siddhesh.QuickCart.Entity.OrderStatus;
import com.siddhesh.QuickCart.kafka.DeliveryProducer;
import com.siddhesh.QuickCart.kafka.event.OrderAssignedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderAssignmentService {
    private final DeliveryProducer deliveryProducer;

    public void assignPartner(Order order) {
        String partnerName = "Rahul";
        String partnerPhone = "9876543210";

        order.setStatus(OrderStatus.ASSIGNED);
        OrderAssignedEvent event = new OrderAssignedEvent(
                order.getId(),
                order.getUser().getId(),
                partnerName,
                partnerPhone
        );
        deliveryProducer.publishOrderAssignedEvent(event);
    }
}
