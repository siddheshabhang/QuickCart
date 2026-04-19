package com.quickcart.feign;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.common.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {

    @PutMapping("/products/{id}/deduct-stock")
    ApiResponse<Void> deductStock(@PathVariable("id") Long productId, @RequestParam("quantity") int quantity);
}
