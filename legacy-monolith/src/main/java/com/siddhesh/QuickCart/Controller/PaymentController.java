package com.siddhesh.QuickCart.Controller;

import com.siddhesh.QuickCart.Dto.ApiResponse;
import com.siddhesh.QuickCart.Dto.PaymentResponseDto;
import com.siddhesh.QuickCart.Service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/process/{order_id}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> processPayment(
            @PathVariable Long order_id,
            @RequestParam boolean success) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Payment processed", paymentService.processPayment(order_id, success))
        );
    }
}
