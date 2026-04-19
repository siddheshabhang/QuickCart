package com.quickcart.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CartResponseDto {
    private List<CartItemDto> items;
    private Double totalAmount;
}
