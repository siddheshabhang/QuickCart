package com.siddhesh.QuickCart.Service;

import com.siddhesh.QuickCart.Dto.PaymentResponseDto;
import com.siddhesh.QuickCart.Entity.*;
import com.siddhesh.QuickCart.Exception.ResourceNotFoundException;
import com.siddhesh.QuickCart.Mapper.PaymentMapper;
import com.siddhesh.QuickCart.Repository.OrderRepository;
import com.siddhesh.QuickCart.Repository.PaymentRepository;
import com.siddhesh.QuickCart.Repository.ProductRepository;
import com.siddhesh.QuickCart.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final PaymentMapper paymentMapper;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public PaymentResponseDto processPayment(Long orderId, boolean simulateSuccess) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        User currentUser = getCurrentUser();
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized payment attempt");
        }

        // Check existing payment
        Payment payment = paymentRepository.findByOrder(order).orElse(null);

        if (payment != null) {
            if (payment.getStatus() == PaymentStatus.SUCCESS)
                return paymentMapper.toDto(payment);
        } else {
            // Create new payment only when none exists
            payment = Payment.builder()
                    .order(order)
                    .amount(order.getTotalAmount())
                    .status(PaymentStatus.PENDING)
                    .transactionId(UUID.randomUUID().toString())
                    .build();
            payment = paymentRepository.save(payment);
        }

        order.setStatus(OrderStatus.PAYMENT_PENDING);

        // Process Payment
        if (simulateSuccess) {
            // If retry after failure → re-deduct stock
            if (payment.getStatus() == PaymentStatus.FAILED) {
                deductStock(order);
            }
            payment.setStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.PAID);
        } else {
            if (payment.getStatus() != PaymentStatus.FAILED) {
                restoreStock(order);
            }
            payment.setStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.FAILED);
        }
        payment = paymentRepository.save(payment);
        return paymentMapper.toDto(payment);
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

    private void deductStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + item.getProductId()
                    ));
            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock on retry");
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }
    }
}
