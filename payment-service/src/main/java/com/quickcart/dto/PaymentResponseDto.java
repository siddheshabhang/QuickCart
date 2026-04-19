package com.quickcart.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {
    private Long paymentId;
    private Long orderId;
    private Double amount;
    private String status;
    private String transactionId;
}