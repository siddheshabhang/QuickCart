package com.quickcart.service;

import com.quickcart.common.event.PaymentCompletedEvent;
import com.quickcart.dto.OrderResponseDto;
import com.quickcart.dto.OrderStatus;
import com.quickcart.dto.PaymentRequestDto;
import com.quickcart.dto.PaymentResponseDto;
import com.quickcart.entity.Payment;
import com.quickcart.entity.PaymentStatus;
import com.quickcart.feign.OrderClient;
import com.quickcart.kafka.PaymentProducer;
import com.quickcart.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final PaymentProducer paymentProducer;

    private Long getCurrentUserId() {
        String principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return Long.parseLong(principal);
    }

    public PaymentResponseDto processPayment(PaymentRequestDto requestDto) {
        Long userId = getCurrentUserId();

        OrderResponseDto order = orderClient.getOrderById(requestDto.getOrderId()).getData();
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(requestDto.getOrderId());

        if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentStatus.SUCCESS) {
            return toDto(existingPayment.get());
        }

        Payment payment = existingPayment.orElseGet(() -> paymentRepository.save(
                Payment.builder()
                        .orderId(requestDto.getOrderId())
                        .amount(order.getTotalAmount())
                        .status(PaymentStatus.PENDING)
                        .transactionId(UUID.randomUUID().toString())
                        .build()
        ));

        if (requestDto.isSimulateSuccess()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            orderClient.updateOrderStatus(requestDto.getOrderId(), OrderStatus.CONFIRMED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            orderClient.updateOrderStatus(requestDto.getOrderId(), OrderStatus.FAILED);
        }

        paymentRepository.save(payment);

        paymentProducer.publishPaymentCompleted(PaymentCompletedEvent.builder()
                .orderId(requestDto.getOrderId())
                .userId(order.getUserId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .build());
        return toDto(payment);
    }

    private PaymentResponseDto toDto(Payment payment) {
        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .build();
    }
}
