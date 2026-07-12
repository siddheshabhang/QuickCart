package com.quickcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    private Long orderId;
    private Double totalAmount;
    private String status;
    private Long userId;
    private Long storeId;
    private List<OrderItemDto> items;
}
