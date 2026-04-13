package com.quickcart.dto;

import lombok.Value;
import java.time.LocalDateTime;

@Value
public class ProductResponseDto {
    Long id;
    String name;
    double price;
    String description;
    Integer stock;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

