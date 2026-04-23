package com.quickcart.service;


import com.quickcart.common.event.OrderCreatedEvent;
import com.quickcart.common.exception.ResourceNotFoundException;
import com.quickcart.dto.*;
import com.quickcart.entity.Order;
import com.quickcart.entity.OrderItem;
import com.quickcart.entity.OrderStatus;
import com.quickcart.feign.CartClient;
import com.quickcart.feign.ProductClient;
import com.quickcart.kafka.OrderProducer;
import com.quickcart.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class
OrderService {
    private final OrderRepository orderRepository;
    private final CartHelperService cartHelperService;
    private final ProductHelperService productHelperService;
    private final OrderProducer orderProducer;

    private Long getCurrentUserId() {
        String principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return Long.parseLong(principal);
    }

    @Transactional
    public OrderResponseDto placeOrder(OrderRequestDto requestDto) {
        Long userId = getCurrentUserId();

        CartResponseDto cart = cartHelperService.getCart().getData();

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0;

        for (CartItemDto cartItem : cart.getItems()) {
            productHelperService.deductStock(cartItem.getProductId(), cartItem.getQuantity());

            orderItems.add(OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .price(cartItem.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build());

            total += cartItem.getPrice() * cartItem.getQuantity();
        }

        Order order = Order.builder()
                .userId(userId)
                .totalAmount(total)
                .items(orderItems)
                .status(OrderStatus.CREATED)
                .address(requestDto.getAddress())
                .phoneNumber(requestDto.getPhoneNumber())
                .build();

        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }

        Order savedOrder = orderRepository.save(order);

        cartHelperService.clearCart();

        orderProducer.publishOrderCreated(OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .userId(userId)
                .totalAmount(savedOrder.getTotalAmount())
                .build());

        return toDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getUserOrders() {
        Long userId = getCurrentUserId();
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private OrderResponseDto toDto(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(item -> OrderItemDto.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        return OrderResponseDto.builder()
                .orderId(order.getId())
                .totalAmount(order.getTotalAmount())
                .items(items)
                .status(order.getStatus())
                .build();
    }

    public OrderResponseDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return toDto(order);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        order.setStatus(status);
        orderRepository.save(order);
    }

}
