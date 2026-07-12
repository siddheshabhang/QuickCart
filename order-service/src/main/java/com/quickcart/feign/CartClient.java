package com.quickcart.feign;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.dto.CartResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "CART-SERVICE")
public interface CartClient {

    @GetMapping("/cart")
    ApiResponse<CartResponseDto> getCart(@RequestHeader(value = "X-Store-Id", required = false) Long storeId);

    @DeleteMapping("/cart/clear")
    ApiResponse<Void> clearCart();
}
