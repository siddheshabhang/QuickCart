package com.siddhesh.QuickCart.Controller;

import com.siddhesh.QuickCart.Dto.ApiResponse;
import com.siddhesh.QuickCart.Entity.OrderStatus;
import com.siddhesh.QuickCart.Service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService deliveryService;

    @PostMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        deliveryService.updateStatus(orderId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Status updated to " + status, null));
    }
}
