package com.siddhesh.QuickCart.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponseDto {
    private Long paymentId;
    private Long orderId;
    private Double amount;
    private String status;
    private String transactionId;
}
