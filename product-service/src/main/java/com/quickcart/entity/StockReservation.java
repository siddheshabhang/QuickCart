package com.quickcart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a soft-lock on a product's stock placed when an order is created.
 *
 * Flow:
 *  1. Order created → reservation saved with status RESERVED, expiresAt = now + TTL
 *  2. Payment SUCCESS → status transitions to CONFIRMED (stock is permanently deducted)
 *  3. Payment FAILED  → status transitions to RELEASED (stock is returned to pool)
 *  4. No payment in time → @Scheduled job sets status to EXPIRED and returns stock
 */
@Entity
@Table(name = "stock_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The product whose stock is being reserved. */
    @Column(nullable = false)
    private Long productId;

    /** The order that triggered this reservation. */
    @Column(nullable = false)
    private Long orderId;

    /** The dark store from which this product is being reserved. */
    @Column(nullable = false)
    private Long storeId;

    /** How many units are being held. */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Current lifecycle state of the reservation.
     * Stored as a String so new enum values don't require a DB migration.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    /** When the reservation was created. */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Deadline by which payment must be confirmed.
     * After this timestamp, the scheduler will mark the reservation EXPIRED
     * and return the stock to the available pool.
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
