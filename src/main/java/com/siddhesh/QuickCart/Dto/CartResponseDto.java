package com.siddhesh.QuickCart.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CartResponseDto {
    private List<CartItemDto> items;
    private Double totalAmount;
}
