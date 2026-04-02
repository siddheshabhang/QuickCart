package com.siddhesh.QuickCart.Service;

import com.siddhesh.QuickCart.Dto.PaymentResponseDto;
import com.siddhesh.QuickCart.Entity.*;
import com.siddhesh.QuickCart.Exception.ResourceNotFoundException;
import com.siddhesh.QuickCart.Repository.OrderRepository;
import com.siddhesh.QuickCart.Repository.PaymentRepository;
import com.siddhesh.QuickCart.Repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;

    @Transactional
    public PaymentResponseDto processPayment(Long orderId, boolean simulateSuccess) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .transactionId(UUID.randomUUID().toString())
                .build();

        paymentRepository.save(payment);
        if(simulateSuccess) {
            payment.setStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            restoreStock(order);
        }
        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .orderId(orderId)
                .amount(order.getTotalAmount())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .build();
    }

    private void restoreStock(Order order) {
        for(OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + item.getProductId()));
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
    }
}
