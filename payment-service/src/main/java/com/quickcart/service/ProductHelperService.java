package com.quickcart.service;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.common.event.OrderCreatedEvent;
import com.quickcart.feign.ProductClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductHelperService {

    private final ProductClient productClient;

    @CircuitBreaker(name = "productService", fallbackMethod = "reserveStockFallback")
    public ApiResponse<Void> reserveStock(OrderCreatedEvent event) {
        return productClient.reserveStock(event);
    }

    public ApiResponse<Void> reserveStockFallback(OrderCreatedEvent event, Throwable ex) {
        Long orderId = event != null ? event.getOrderId() : null;
        throw new RuntimeException("Product service unavailable. Stock could not be reserved for order: " + orderId);
    }
}
