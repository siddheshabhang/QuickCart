package com.quickcart.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tracks stock quantity for a specific (store, product) pair.
 *
 * Replaces the global {@code stock} field previously on {@link Product}.
 * Each dark store has its own independent inventory for every product.
 * A product may be in-stock at Store A but out-of-stock at Store B.
 *
 * <h3>Key invariant</h3>
 * {@code quantity} must never go below zero. Decrement is only allowed
 * via {@link com.quickcart.service.StockReservationService} which checks
 * sufficient stock before committing the deduction.
 */
@Entity
@Table(
    name = "inventory",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_inventory_store_product",
        columnNames = {"store_id", "product_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The dark store this inventory row belongs to. */
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /** The product being tracked. */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_inventory_product"))
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Product product;

    /** Units currently available for sale at this store. */
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;
}
