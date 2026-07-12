package com.quickcart.service;

import com.quickcart.common.event.OrderCreatedEvent;
import com.quickcart.common.event.OrderItemEvent;
import com.quickcart.common.event.PaymentCompletedEvent;
import com.quickcart.dto.OrderResponseDto;
import com.quickcart.dto.OrderStatus;
import com.quickcart.dto.PaymentRequestDto;
import com.quickcart.dto.PaymentResponseDto;
import com.quickcart.entity.Payment;
import com.quickcart.entity.PaymentStatus;
import com.quickcart.kafka.PaymentProducer;
import com.quickcart.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderHelperService orderHelperService;
    private final ProductHelperService productHelperService;
    private final PaymentProducer paymentProducer;

    private Long getCurrentUserId() {
        String principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return Long.parseLong(principal);
    }

    /**
     * Reads the authenticated user's email from the SecurityContext credentials.
     * Stored there by GatewayAuthFilter — no Feign call needed.
     */
    private String getCurrentUserEmail() {
        return (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getCredentials();
    }

    public PaymentResponseDto processPayment(PaymentRequestDto requestDto) {
        return processPayment(requestDto, null);
    }

    public PaymentResponseDto processPayment(PaymentRequestDto requestDto, String idempotencyKey) {
        Long userId = getCurrentUserId();
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

        if (normalizedIdempotencyKey != null) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(normalizedIdempotencyKey);
            if (existing.isPresent()) return toDto(existing.get());
        }

        OrderResponseDto order = orderHelperService.getOrderById(requestDto.getOrderId()).getData();
        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only pay for your own order");
        }

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
                        .idempotencyKey(normalizedIdempotencyKey)
                        .build()
        ));

        if (requestDto.isSimulateSuccess()) {
            if (existingPayment
                    .map(existing -> existing.getStatus() == PaymentStatus.FAILED)
                    .orElse(false)) {
                productHelperService.reserveStock(toStockReservationEvent(order));
            }
            payment.setStatus(PaymentStatus.SUCCESS);
            orderHelperService.updateOrderStatus(requestDto.getOrderId(), OrderStatus.CONFIRMED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            orderHelperService.updateOrderStatus(requestDto.getOrderId(), OrderStatus.FAILED);
        }

        paymentRepository.save(payment);

        paymentProducer.publishPaymentCompleted(PaymentCompletedEvent.builder()
                .orderId(requestDto.getOrderId())
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .userEmail(getCurrentUserEmail())   // carried from JWT via SecurityContext
                .build());
        return toDto(payment);
    }

    private OrderCreatedEvent toStockReservationEvent(OrderResponseDto order) {
        List<OrderItemEvent> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                        .map(item -> new OrderItemEvent(item.getProductId(), item.getQuantity()))
                        .toList();

        return OrderCreatedEvent.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .totalAmount(order.getTotalAmount())
                .userEmail(getCurrentUserEmail())
                .items(items)
                .build();
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

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return idempotencyKey.trim();
    }
}
