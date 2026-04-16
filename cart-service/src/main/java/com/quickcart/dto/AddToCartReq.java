package com.quickcart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class AddToCartReq {

    @NotNull(message = "Product ID must not be null")
    Long productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    int quantity;
}
