package com.quickcart.service;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.feign.ProductClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductHelperService {
    private final ProductClient productClient;

    @CircuitBreaker(name = "productService", fallbackMethod = "deductStockFallback")
    public ApiResponse<Void> deductStock(Long productId, int quantity) {
        return productClient.deductStock(productId, quantity);
    }

    public ApiResponse<Void> deductStockFallback(Long productId, int quantity, Throwable ex) {
        throw new RuntimeException("Product service unavailable. Stock could not be deducted for product: " + productId);
    }
}
