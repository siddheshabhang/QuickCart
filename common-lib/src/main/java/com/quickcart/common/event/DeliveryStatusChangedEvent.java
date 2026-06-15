package com.quickcart.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryStatusChangedEvent {
    private Long orderId;
    private Long userId;
    private String status;
    private String userEmail;
}
