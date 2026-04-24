package com.quickcart.service;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.common.dto.ProductResponseDto;
import com.quickcart.common.exception.ResourceNotFoundException;
import com.quickcart.feign.ProductClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductHelperService {

    private final ProductClient productClient;

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    @Retry(name = "productService")
    public ApiResponse<ProductResponseDto> getProductById(Long productId) {
        return productClient.getProductById(productId);
    }

    public ApiResponse<ProductResponseDto> getProductFallback(Long productId, Throwable ex) {
        if (ex instanceof FeignException.NotFound) {
            throw new ResourceNotFoundException("Product not found with Id " + productId);
        }
        throw new RuntimeException("Product service is unavailable. Please try again later.");
    }
}
