package com.quickcart.controller;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.dto.OrderRequestDto;
import com.quickcart.dto.OrderResponseDto;
import com.quickcart.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/place")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponseDto>> placeOrder(
            @RequestBody OrderRequestDto requestDto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Order placed", orderService.placeOrder(requestDto)));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getOrders() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Orders fetched", orderService.getUserOrders())
        );
    }

}
