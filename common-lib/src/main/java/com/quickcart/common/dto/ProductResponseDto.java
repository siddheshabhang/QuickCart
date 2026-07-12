package com.quickcart.common.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Shared DTO representing a product — used by product-service (to produce)
 * and cart-service / other services (to consume via Feign clients).
 *
 * NOTE: Must NOT use @Value here. @Value makes all fields final which
 * prevents Jackson from deserializing it in Feign clients (no no-arg
 * constructor / setters). Use @Getter + @Setter + @NoArgsConstructor instead.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDto {
    private Long id;
    private String name;
    private double price;
    private String description;
    private String imageUrl;
    private Integer stock;
    private boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
