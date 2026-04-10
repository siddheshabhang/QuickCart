package com.siddhesh.QuickCart.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemDto {
    private Long productId;
    private String productName;
    private Double price;
    private Integer quantity;
}
