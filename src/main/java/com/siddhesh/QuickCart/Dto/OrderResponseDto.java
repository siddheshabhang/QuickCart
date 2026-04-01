package com.siddhesh.QuickCart.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderResponseDto {
    private Long orderId;
    private Double totalAmount;
    private List<OrderItemDto> items;
}
