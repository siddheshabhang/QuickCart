package com.quickcart.service;

import com.quickcart.common.exception.ResourceNotFoundException;
import com.quickcart.entity.Product;
import com.quickcart.entity.ReservationStatus;
import com.quickcart.entity.StockReservation;
import com.quickcart.repository.ProductRepository;
import com.quickcart.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationService {

    /** How long a reservation is kept alive waiting for payment confirmation. */
    private static final int RESERVATION_TTL_MINUTES = 10;

    private final StockReservationRepository reservationRepository;
    private final ProductRepository productRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new RESERVED reservation for the given order.
     *
     * <p>The method deducts {@code quantity} from the product's physical stock
     * immediately so that two concurrent orders cannot over-sell the same item.
     * If payment never arrives, the scheduler will restore the stock and mark
     * the reservation EXPIRED.
     *
     * @param productId product whose stock to soft-lock
     * @param orderId   order that triggered the reservation
     * @param quantity  number of units to hold
     * @return the persisted {@link StockReservation}
     */
    @Transactional
    public StockReservation reserve(Long productId, Long orderId, int quantity) {
        Product product = findProduct(productId);
        product.deductStock(quantity);          // throws if insufficient stock
        productRepository.save(product);

        StockReservation reservation = StockReservation.builder()
                .productId(productId)
                .orderId(orderId)
                .quantity(quantity)
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(RESERVATION_TTL_MINUTES))
                .build();

        StockReservation saved = reservationRepository.save(reservation);
        log.info("Stock RESERVED → orderId: {}, productId: {}, qty: {}, expiresAt: {}",
                orderId, productId, quantity, saved.getExpiresAt());
        return saved;
    }

    /**
     * Confirms a reservation when payment succeeds.
     *
     * <p>Stock was already physically deducted at reservation time, so nothing
     * more needs to happen to the product row. We just flip the status to
     * CONFIRMED so the scheduler won't try to release this reservation.
     *
     * @param orderId order whose reservation should be confirmed
     */
    @Transactional
    public void confirmReservation(Long orderId) {
        StockReservation reservation = findReservationByOrderId(orderId);

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            log.warn("Cannot confirm reservation for orderId: {} — current status: {}",
                    orderId, reservation.getStatus());
            return;
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
        log.info("Stock CONFIRMED → orderId: {}, productId: {}, qty: {}",
                orderId, reservation.getProductId(), reservation.getQuantity());
    }

    /**
     * Releases a reservation when payment fails.
     *
     * <p>The previously deducted stock is returned to the product's available pool.
     *
     * @param orderId order whose reservation should be released
     */
    @Transactional
    public void releaseReservation(Long orderId) {
        StockReservation reservation = findReservationByOrderId(orderId);

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            log.warn("Cannot release reservation for orderId: {} — current status: {}",
                    orderId, reservation.getStatus());
            return;
        }

        restoreStock(reservation);
        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);
        log.info("Stock RELEASED → orderId: {}, productId: {}, qty: {} returned",
                orderId, reservation.getProductId(), reservation.getQuantity());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Called by the scheduler
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Finds all RESERVED reservations whose TTL has passed, restores their stock,
     * and marks them EXPIRED.
     *
     * <p>Invoked periodically by {@link com.quickcart.scheduler.ReservationCleanupJob}.
     */
    @Transactional
    public void expireStaleReservations() {
        List<StockReservation> stale = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.RESERVED, LocalDateTime.now());

        if (stale.isEmpty()) {
            log.debug("No stale reservations found.");
            return;
        }

        log.info("Expiring {} stale reservation(s)...", stale.size());

        for (StockReservation reservation : stale) {
            try {
                restoreStock(reservation);
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);
                log.info("Reservation EXPIRED → orderId: {}, productId: {}, qty: {} returned",
                        reservation.getOrderId(), reservation.getProductId(), reservation.getQuantity());
            } catch (Exception e) {
                log.error("Failed to expire reservation id={} for orderId={}",
                        reservation.getId(), reservation.getOrderId(), e);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void restoreStock(StockReservation reservation) {
        Product product = findProduct(reservation.getProductId());
        product.setStock(product.getStock() + reservation.getQuantity());
        productRepository.save(product);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with Id " + productId));
    }

    private StockReservation findReservationByOrderId(Long orderId) {
        return reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock reservation not found for orderId: " + orderId));
    }
}
