package com.quickcart.controller;

import com.quickcart.common.dto.ApiResponse;

import com.quickcart.entiry.DeliveryStatus;
import com.quickcart.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService deliveryService;

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long orderId,
            @RequestParam DeliveryStatus status) {
        deliveryService.updateStatus(orderId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Status updated to " + status, null));
    }

    @PostMapping("/{orderId}/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @PathVariable Long orderId,
            @RequestParam String otp) {
        deliveryService.verifyOtp(orderId, otp);
        return ResponseEntity.ok(new ApiResponse<>(true, "Order delivered successfully", null));
    }
}