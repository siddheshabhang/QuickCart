package com.quickcart.repository;

import com.quickcart.entity.ReservationStatus;
import com.quickcart.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    /**
     * Used by the Kafka consumer to look up a reservation when a
     * payment event arrives for a given order.
     */
    Optional<StockReservation> findByOrderId(Long orderId);

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
