package com.siddhesh.QuickCart.Mapper;

import com.siddhesh.QuickCart.Dto.PaymentResponseDto;
import com.siddhesh.QuickCart.Entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentResponseDto toDto(Payment payment) {
        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .build();
    }
}
