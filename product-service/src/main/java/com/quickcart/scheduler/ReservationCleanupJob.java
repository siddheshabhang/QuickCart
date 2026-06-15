package com.quickcart.scheduler;

import com.quickcart.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background job that periodically scans for stock reservations whose TTL
 * has elapsed without a payment confirmation.
 *
 * <p>For each expired reservation the job:
 * <ol>
 *   <li>Restores the reserved quantity back to the product's available stock</li>
 *   <li>Transitions the reservation status from RESERVED → EXPIRED</li>
 * </ol>
 *
 * <p>The fixed-delay of 60 seconds means the job waits 60 s after the previous
 * run finishes before starting again, preventing overlapping executions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupJob {

    private final StockReservationService stockReservationService;

    /**
     * Runs every 60 seconds (after the previous run completes).
     *
     * <p>fixedDelay  — next run starts 60 s after the previous run ends<br>
     * initialDelay — wait 30 s after application startup before the first run,
     *                giving the app time to fully initialise.
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void releaseExpiredReservations() {
        log.info("ReservationCleanupJob triggered — scanning for expired reservations...");
        try {
            stockReservationService.expireStaleReservations();
        } catch (Exception e) {
            log.error("ReservationCleanupJob encountered an unexpected error", e);
        }
    }
}
