package com.siddhesh.QuickCart.Mapper;

import com.siddhesh.QuickCart.Dto.OrderItemDto;
import com.siddhesh.QuickCart.Dto.OrderResponseDto;
import com.siddhesh.QuickCart.Entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponseDto toDto(Order order) {
        return OrderResponseDto.builder()
                .orderId(order.getId())
                .totalAmount(order.getTotalAmount())
                .items(order.getItems().stream()
                        .map(item -> OrderItemDto.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .build();
    }
}