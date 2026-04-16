package com.quickcart.common.dto;

import lombok.Value;
import java.time.LocalDateTime;

/**
 * Shared DTO representing a product — used by product-service (to produce)
 * and cart-service / other services (to consume via Feign clients).
 */
@Value
public class ProductResponseDto {
    Long id;
    String name;
    double price;
    String description;
    Integer stock;
    boolean available;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
