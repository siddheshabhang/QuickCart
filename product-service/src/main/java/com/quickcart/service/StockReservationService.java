package com.quickcart.service;

import com.quickcart.common.event.OrderCreatedEvent;
import com.quickcart.common.event.OrderItemEvent;
import com.quickcart.common.exception.ResourceNotFoundException;
import com.quickcart.entity.Inventory;
import com.quickcart.entity.ReservationStatus;
import com.quickcart.entity.StockReservation;
import com.quickcart.repository.InventoryRepository;
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
    private final InventoryRepository inventoryRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reserves every item in an order. Product-service owns the full stock
     * mutation here: check inventory, decrement available stock, and create
     * reservation rows for payment confirmation or compensation.
     */
    @Transactional
    public List<StockReservation> reserveOrder(OrderCreatedEvent event) {
        validateOrderEvent(event);

        List<StockReservation> existing = reservationRepository.findAllByOrderId(event.getOrderId());
        if (!existing.isEmpty()) {
            boolean activeReservation = existing.stream()
                    .allMatch(reservation -> reservation.getStatus() == ReservationStatus.RESERVED
                            || reservation.getStatus() == ReservationStatus.CONFIRMED);
            if (activeReservation) {
                log.info("Stock reservation already exists for orderId: {} — treating reserve as idempotent",
                        event.getOrderId());
                return existing;
            }

            boolean canReReserve = existing.stream()
                    .allMatch(reservation -> reservation.getStatus() == ReservationStatus.RELEASED
                            || reservation.getStatus() == ReservationStatus.EXPIRED);
            if (canReReserve) {
                return reReserve(existing);
            }

            throw new IllegalArgumentException("Stock reservation has an invalid mixed state for orderId: "
                    + event.getOrderId());
        }

        return event.getItems().stream()
                .map(item -> reserve(
                        item.getProductId(),
                        event.getOrderId(),
                        item.getQuantity(),
                        event.getStoreId()))
                .toList();
    }

    /**
     * Creates a reservation for a product at a specific dark store and deducts
     * that quantity from available inventory in the same transaction.
     */
    @Transactional
    public StockReservation reserve(Long productId, Long orderId, int quantity, Long storeId) {
        validateReservation(productId, orderId, quantity, storeId);

        Inventory inventory = inventoryRepository
                .findByStoreIdAndProductIdForUpdate(storeId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for storeId=" + storeId + " productId=" + productId));

        if (inventory.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for productId=" + productId + " at storeId=" + storeId);
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);

        StockReservation reservation = StockReservation.builder()
                .productId(productId)
                .orderId(orderId)
                .quantity(quantity)
                .storeId(storeId)
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(RESERVATION_TTL_MINUTES))
                .build();

        StockReservation saved = reservationRepository.save(reservation);
        log.info("Stock RESERVED → orderId: {}, productId: {}, storeId: {}, qty: {}, expiresAt: {}",
                orderId, productId, storeId, quantity, saved.getExpiresAt());
        return saved;
    }

    /**
     * Confirms all reservations for an order when payment succeeds. Stock was
     * deducted when the reservation was created, so confirmation only finalizes
     * the lifecycle state and prevents expiry/release.
     */
    @Transactional
    public void confirmReservations(Long orderId) {
        List<StockReservation> reservations = findReservationsByOrderId(orderId);

        for (StockReservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
                continue;
            }

            if (reservation.getStatus() != ReservationStatus.RESERVED) {
                log.warn("Cannot confirm reservation for orderId: {}, productId: {} — current status: {}",
                        orderId, reservation.getProductId(), reservation.getStatus());
                continue;
            }

            reservation.setStatus(ReservationStatus.CONFIRMED);
        }

        reservationRepository.saveAll(reservations);
        log.info("Stock CONFIRMED → orderId: {}, reservations: {}", orderId, reservations.size());
    }

    /**
     * Releases all reservations when payment fails or checkout compensation is
     * needed. Only RESERVED rows restore stock; terminal rows are left untouched.
     */
    @Transactional
    public void releaseReservations(Long orderId) {
        List<StockReservation> reservations = findReservationsByOrderId(orderId);

        int released = 0;
        for (StockReservation reservation : reservations) {
            if (reservation.getStatus() != ReservationStatus.RESERVED) {
                log.warn("Cannot release reservation for orderId: {}, productId: {} — current status: {}",
                        orderId, reservation.getProductId(), reservation.getStatus());
                continue;
            }

            restoreInventory(reservation);
            reservation.setStatus(ReservationStatus.RELEASED);
            released++;
        }

        reservationRepository.saveAll(reservations);
        log.info("Stock RELEASED → orderId: {}, reservations released: {}", orderId, released);
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
                restoreInventory(reservation);
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);
                log.info("Reservation EXPIRED → orderId: {}, productId: {}, storeId: {}, qty: {} returned",
                        reservation.getOrderId(), reservation.getProductId(),
                        reservation.getStoreId(), reservation.getQuantity());
            } catch (Exception e) {
                log.error("Failed to expire reservation id={} for orderId={}",
                        reservation.getId(), reservation.getOrderId(), e);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns reserved stock to the specific dark store's Inventory row.
     * This is the SAGA compensation step — must target the exact (storeId, productId) pair.
     */
    private void restoreInventory(StockReservation reservation) {
        Inventory inventory = inventoryRepository
                .findByStoreIdAndProductIdForUpdate(reservation.getStoreId(), reservation.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for storeId=" + reservation.getStoreId()
                                + " productId=" + reservation.getProductId()));

        inventory.setQuantity(inventory.getQuantity() + reservation.getQuantity());
        inventoryRepository.save(inventory);
    }

    private List<StockReservation> reReserve(List<StockReservation> reservations) {
        LocalDateTime newExpiry = LocalDateTime.now().plusMinutes(RESERVATION_TTL_MINUTES);

        for (StockReservation reservation : reservations) {
            Inventory inventory = inventoryRepository
                    .findByStoreIdAndProductIdForUpdate(reservation.getStoreId(), reservation.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for storeId=" + reservation.getStoreId()
                                    + " productId=" + reservation.getProductId()));

            if (inventory.getQuantity() < reservation.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for productId=" + reservation.getProductId()
                                + " at storeId=" + reservation.getStoreId());
            }

            inventory.setQuantity(inventory.getQuantity() - reservation.getQuantity());
            inventoryRepository.save(inventory);

            reservation.setStatus(ReservationStatus.RESERVED);
            reservation.setExpiresAt(newExpiry);
        }

        List<StockReservation> saved = reservationRepository.saveAll(reservations);
        Long orderId = reservations.get(0).getOrderId();
        log.info("Stock RE-RESERVED → orderId: {}, reservations: {}", orderId, saved.size());
        return saved;
    }

    private List<StockReservation> findReservationsByOrderId(Long orderId) {
        List<StockReservation> reservations = reservationRepository.findAllByOrderId(orderId);
        if (reservations.isEmpty()) {
            throw new ResourceNotFoundException("Stock reservation not found for orderId: " + orderId);
        }
        return reservations;
    }

    private void validateOrderEvent(OrderCreatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Reservation request is required");
        }
        if (event.getOrderId() == null) {
            throw new IllegalArgumentException("Order id is required for stock reservation");
        }
        if (event.getStoreId() == null) {
            throw new IllegalArgumentException("Store id is required for stock reservation");
        }
        if (event.getItems() == null || event.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one order item is required for stock reservation");
        }
        for (OrderItemEvent item : event.getItems()) {
            validateReservation(item.getProductId(), event.getOrderId(), item.getQuantity(), event.getStoreId());
        }
    }

    private void validateReservation(Long productId, Long orderId, Integer quantity, Long storeId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product id is required for stock reservation");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order id is required for stock reservation");
        }
        if (storeId == null) {
            throw new IllegalArgumentException("Store id is required for stock reservation");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive");
        }
    }
}
