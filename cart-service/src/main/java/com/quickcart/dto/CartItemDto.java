package com.quickcart.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CartItemDto {

    Long cartItemId;
    Long productId;
    String productName;
    double price;
    int quantity;
    double itemTotal;
}
