package com.quickcart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a product in the QuickCart catalogue.
 *
 * Stock is NOT stored here — it lives in {@link Inventory} as a
 * per-dark-store quantity. Use {@link com.quickcart.repository.InventoryRepository}
 * to query available stock for a specific store.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String description;
    private String imageUrl;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
