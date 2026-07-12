package com.quickcart.feign;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.common.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {

    @GetMapping("/products/{id}")
    ApiResponse<ProductResponseDto> getProductById(
            @PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Store-Id", required = false) Long storeId);
}
