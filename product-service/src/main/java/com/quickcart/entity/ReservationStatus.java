package com.quickcart.entity;

/**
 * Lifecycle of a stock reservation:
 *
 *   RESERVED  → stock is soft-locked (deducted from available stock temporarily)
 *   CONFIRMED → payment succeeded; reservation is finalised (stock permanently deducted)
 *   RELEASED  → payment failed or was cancelled; reserved stock is returned to available pool
 *   EXPIRED   → no payment arrived before the TTL; scheduler released the held stock
 */
public enum ReservationStatus {
    RESERVED,
    CONFIRMED,
    RELEASED,
    EXPIRED
}
