package com.siddhesh.QuickCart.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.siddhesh.QuickCart.Entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryStatusChangedEvent {
    private Long orderId;
    private Long userId;

    private OrderStatus status;
}