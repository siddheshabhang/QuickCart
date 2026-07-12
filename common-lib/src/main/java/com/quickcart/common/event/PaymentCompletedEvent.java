package com.quickcart.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {
    private Long orderId;
    private Long userId;
    private String status;
    private Double amount;
    private String userEmail;
    /** The dark store that fulfilled this order — needed for SAGA stock compensation. */
    private Long storeId;
}