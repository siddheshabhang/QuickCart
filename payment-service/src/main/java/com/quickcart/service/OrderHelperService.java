package com.quickcart.service;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.dto.OrderResponseDto;
import com.quickcart.dto.OrderStatus;
import com.quickcart.feign.OrderClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderHelperService {
    private final OrderClient orderClient;

    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrderFallback")
    @Retry(name = "orderServiceRead")
    public ApiResponse<OrderResponseDto> getOrderById(Long orderId) {
        return orderClient.getOrderById(orderId);
    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "updateStatusFallback")
    public ApiResponse<Void> updateOrderStatus(Long orderId, OrderStatus status) {
        return orderClient.updateOrderStatus(orderId, status);
    }

    public ApiResponse<OrderResponseDto> getOrderFallback(Long orderId, Throwable ex) {
        throw new RuntimeException("Order service unavailable. Cannot fetch order: " + orderId);
    }

    public ApiResponse<Void> updateStatusFallback(Long orderId, OrderStatus status, Throwable ex) {
        throw new RuntimeException("Order service unavailable. Cannot update status for order: " + orderId);
    }
}
