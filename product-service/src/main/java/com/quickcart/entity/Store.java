package com.quickcart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a physical QuickCart dark store.
 *
 * Each store has a GPS coordinate and a service radius (in km).
 * The nearest active store to the customer's location is found
 * using Redis GEO commands (GEOSEARCH) for sub-millisecond lookups.
 *
 * Inventory (available stock per product) is managed via the
 * {@link Inventory} entity — NOT on this entity directly.
 */
@Entity
@Table(name = "dark_stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable name shown to customers and delivery agents. */
    @Column(nullable = false)
    private String name;

    /** City for display and grouping. */
    @Column(nullable = false)
    private String city;

    /** GPS latitude of the store. */
    @Column(nullable = false)
    private Double latitude;

    /** GPS longitude of the store. */
    @Column(nullable = false)
    private Double longitude;

    /**
     * Serviceable radius in kilometres.
     * Standard quick-commerce: 3.5–5 km for guaranteed 10-minute delivery.
     */
    @Column(nullable = false)
    private Double serviceRadiusKm;

    /**
     * When false, the store is excluded from nearest-store lookups.
     * Used to temporarily suspend a store without deleting its inventory.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
