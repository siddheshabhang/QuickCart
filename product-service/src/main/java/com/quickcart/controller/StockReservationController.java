package com.quickcart.controller;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.common.event.OrderCreatedEvent;
import com.quickcart.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock-reservations")
@RequiredArgsConstructor
public class StockReservationController {

    private static final String ORDER_SERVICE = "ORDER-SERVICE";
    private static final String PAYMENT_SERVICE = "PAYMENT-SERVICE";

    private final StockReservationService stockReservationService;

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<Void>> reserveStock(
            @RequestBody OrderCreatedEvent event,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalService) {
        assertInternalService(internalService);
        stockReservationService.reserveOrder(event);
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock reserved", null));
    }

    @PutMapping("/{orderId}/release")
    public ResponseEntity<ApiResponse<Void>> releaseStock(
            @PathVariable("orderId") Long orderId,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalService) {
        assertInternalService(internalService);
        stockReservationService.releaseReservations(orderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock reservation released", null));
    }

    private void assertInternalService(String internalService) {
        if (!ORDER_SERVICE.equals(internalService) && !PAYMENT_SERVICE.equals(internalService)) {
            throw new AccessDeniedException("Stock reservations can only be managed by internal services");
        }
    }
}
