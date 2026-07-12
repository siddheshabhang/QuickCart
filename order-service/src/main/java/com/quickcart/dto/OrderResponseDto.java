package com.quickcart.dto;

import com.quickcart.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderResponseDto {
    private Long orderId;
    private Long userId;
    private Long storeId;
    private Double totalAmount;
    private List<OrderItemDto> items;
    private OrderStatus status;
}
