package com.quickcart.repository;

import com.quickcart.entity.ReservationStatus;
import com.quickcart.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    /**
     * Used to look up all reservations created for an order. A single
     * multi-item order creates one reservation row per product.
     */
    List<StockReservation> findAllByOrderId(Long orderId);

    /**
     * Used by the scheduler to find all RESERVED records whose deadline
     * has already passed — these need to be expired and their stock returned.
     *
     * @param status    must be RESERVED
     * @param threshold anything where expiresAt is before this moment is stale
     */
    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status,
                                                          LocalDateTime threshold);
}
