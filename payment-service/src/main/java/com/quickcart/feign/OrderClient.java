package com.quickcart.feign;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.dto.OrderResponseDto;
import com.quickcart.dto.OrderStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ORDER-SERVICE")
public interface OrderClient {
    @GetMapping("/order/{id}")
    ApiResponse<OrderResponseDto> getOrderById(@PathVariable("id") Long orderId);

    @PutMapping("/order/{id}/status")
    ApiResponse<Void> updateOrderStatus(@PathVariable("id") Long orderId, @RequestParam("status") OrderStatus status);
}
