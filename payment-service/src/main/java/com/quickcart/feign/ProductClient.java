package com.quickcart.feign;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.common.event.OrderCreatedEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {

    @PostMapping("/stock-reservations/reserve")
    ApiResponse<Void> reserveStock(@RequestBody OrderCreatedEvent event);
}
