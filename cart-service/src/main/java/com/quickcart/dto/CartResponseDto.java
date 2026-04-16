package com.quickcart.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CartResponseDto {

    List<CartItemDto> items;
    double totalAmount;
}
