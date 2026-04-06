package com.siddhesh.QuickCart.kafka.event;

import com.siddhesh.QuickCart.Entity.PaymentStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {
    private Long orderId;
    private Long userId;
    private PaymentStatus status;
    private Double amount;
}
