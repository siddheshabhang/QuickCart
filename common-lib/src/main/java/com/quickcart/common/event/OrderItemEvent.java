package com.quickcart.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight representation of a single order line item
 * carried inside {@link OrderCreatedEvent}.
 *
 * product-service uses this to create per-product stock reservations for
 * checkout and to publish a compact order-created event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemEvent {
    private Long productId;
    private Integer quantity;
}
