package com.quickcart.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private Double totalAmount;
    private String userEmail;
    /** The dark store from which this order will be fulfilled. */
    private Long storeId;
    /** Items in the order; product-service reserves one row per item during checkout. */
    private List<OrderItemEvent> items;
}
