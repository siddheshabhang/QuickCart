package com.quickcart.controller;

import com.quickcart.common.dto.ApiResponse;

import com.quickcart.dto.DeliveryResponseDto;
import com.quickcart.entity.DeliveryStatus;
import com.quickcart.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService deliveryService;

    @GetMapping
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<List<DeliveryResponseDto>>> getAllDeliveries() {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Deliveries fetched",
                deliveryService.getAllDeliveries()
        ));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<DeliveryResponseDto>> getDeliveryByOrderId(
            @PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Delivery fetched",
                deliveryService.getDeliveryByOrderId(orderId)
        ));
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable("orderId") Long orderId,
            @RequestParam("status") DeliveryStatus status) {
        deliveryService.updateStatus(orderId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Status updated to " + status, null));
    }

    @PostMapping("/{orderId}/verify-otp")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @PathVariable("orderId") Long orderId,
            @RequestParam("otp") String otp) {
        deliveryService.verifyOtp(orderId, otp);
        return ResponseEntity.ok(new ApiResponse<>(true, "Order delivered successfully", null));
    }
}
