package com.quickcart.service;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.dto.CartResponseDto;
import com.quickcart.feign.CartClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartHelperService {
    private final CartClient cartClient;

    @CircuitBreaker(name = "cartService", fallbackMethod = "getCartFallback")
    @Retry(name = "cartServiceRead")
    public ApiResponse<CartResponseDto> getCart() {
        return cartClient.getCart();
    }

    @CircuitBreaker(name = "cartService", fallbackMethod = "clearCartFallback")
    public ApiResponse<Void> clearCart() {
        return cartClient.clearCart();
    }

    public ApiResponse<CartResponseDto> getCartFallback(Throwable ex) {
        throw new RuntimeException("Cart service unavailable. Please try again later.");
    }

    public ApiResponse<Void> clearCartFallback(Throwable ex) {
        throw new RuntimeException("Cart service unavailable. Cart could not be cleared.");
    }
}
