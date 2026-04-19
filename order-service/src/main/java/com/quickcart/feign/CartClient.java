package com.quickcart.feign;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.dto.CartResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "CART-SERVICE")
public interface CartClient {

    @GetMapping("/cart")
    public ApiResponse<CartResponseDto> getCart();

    @DeleteMapping("/cart/clear")
    public ApiResponse<Void> clearCart();
}
