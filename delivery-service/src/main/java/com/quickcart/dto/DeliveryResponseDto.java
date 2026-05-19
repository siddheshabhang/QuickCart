package com.quickcart.dto;

import com.quickcart.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeliveryResponseDto {
    private Long id;
    private Long orderId;
    private Long userId;
    private DeliveryStatus status;
    private LocalDateTime createdAt;
}
