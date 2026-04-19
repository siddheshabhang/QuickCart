package com.quickcart.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {
    private Long productId;
    private String productName;
    private Double price;
    private Integer quantity;
}
