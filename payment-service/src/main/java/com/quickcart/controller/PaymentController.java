package com.quickcart.controller;


import com.quickcart.common.dto.ApiResponse;
import com.quickcart.dto.PaymentRequestDto;
import com.quickcart.dto.PaymentResponseDto;
import com.quickcart.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/process")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> processPayment(@RequestBody PaymentRequestDto requestDto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Payment processed", paymentService.processPayment(requestDto))
        );
    }
}
