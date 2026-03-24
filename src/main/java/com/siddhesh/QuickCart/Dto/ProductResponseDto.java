package com.siddhesh.QuickCart.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class ProductResponseDto {
    Long id;
    String name;
    double price;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
