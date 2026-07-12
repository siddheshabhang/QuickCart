package com.quickcart.service;

import com.quickcart.common.event.OrderCreatedEvent;
import com.quickcart.common.event.OrderItemEvent;
import com.quickcart.common.event.DeliveryStatusChangedEvent;
import com.quickcart.common.exception.ResourceNotFoundException;
import com.quickcart.dto.*;
import com.quickcart.entity.Order;
import com.quickcart.entity.OrderItem;
import com.quickcart.entity.OrderStatus;
import com.quickcart.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Map<OrderStatus, Integer> STATUS_PROGRESS = new EnumMap<>(OrderStatus.class);

    static {
        STATUS_PROGRESS.put(OrderStatus.CREATED, 0);
        STATUS_PROGRESS.put(OrderStatus.PAYMENT_PENDING, 1);
        STATUS_PROGRESS.put(OrderStatus.CONFIRMED, 2);
        STATUS_PROGRESS.put(OrderStatus.ASSIGNED, 3);
        STATUS_PROGRESS.put(OrderStatus.OUT_FOR_DELIVERY, 4);
        STATUS_PROGRESS.put(OrderStatus.DELIVERED, 5);
        STATUS_PROGRESS.put(OrderStatus.FAILED, 5);
        STATUS_PROGRESS.put(OrderStatus.CANCELLED, 5);
    }

    private final OrderRepository orderRepository;
    private final CartHelperService cartHelperService;
    private final ProductHelperService productHelperService;

    private Long getCurrentUserId() {
        String principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return Long.parseLong(principal);
    }

    /**
     * Reads the authenticated user's email from the SecurityContext.
     * GatewayAuthFilter stores the X-User-Email header as the credentials
     * of the UsernamePasswordAuthenticationToken, so no Feign call is needed.
     */
    private String getCurrentUserEmail() {
        return (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getCredentials();
    }

    @Transactional
    public OrderResponseDto placeOrder(OrderRequestDto requestDto) {
        Long userId = getCurrentUserId();
        Long storeId = requestDto.getStoreId();

        if (storeId == null) {
            throw new IllegalArgumentException("Store id is required to place an order");
        }

        CartResponseDto cart = cartHelperService.getCart(storeId).getData();

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0;

        for (CartItemDto cartItem : cart.getItems()) {
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
                .storeId(storeId)
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

        List<OrderItemEvent> eventItems = orderItems.stream()
                .map(item -> new OrderItemEvent(item.getProductId(), item.getQuantity()))
                .toList();

        OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .userId(userId)
                .totalAmount(savedOrder.getTotalAmount())
                .userEmail(getCurrentUserEmail())
                .storeId(storeId)
                .items(eventItems)
                .build();

        boolean stockReserved = false;
        try {
            productHelperService.reserveStock(orderCreatedEvent);
            stockReserved = true;
            cartHelperService.clearCart();
        } catch (RuntimeException ex) {
            if (stockReserved) {
                releaseStockAfterCheckoutFailure(savedOrder.getId(), ex);
            }
            throw ex;
        }

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
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .totalAmount(order.getTotalAmount())
                .items(items)
                .status(order.getStatus())
                .build();
    }

    public OrderResponseDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        assertOrderBelongsToCurrentUser(order);
        return toDto(order);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        assertOrderBelongsToCurrentUser(order);
        order.setStatus(status);
        orderRepository.save(order);
    }

    @Transactional
    public void syncStatusFromDelivery(DeliveryStatusChangedEvent event) {
        Long orderId = event.getOrderId();
        OrderStatus newStatus = mapDeliveryStatus(event.getStatus())
                .orElse(null);

        if (newStatus == null) {
            log.warn("Ignoring delivery event with unmapped status: {} for orderId: {}",
                    event.getStatus(), orderId);
            return;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (shouldIgnoreDeliveryTransition(order.getStatus(), newStatus)) {
            log.warn("Ignoring stale delivery status sync for orderId: {} currentStatus: {} newStatus: {}",
                    orderId, order.getStatus(), newStatus);
            return;
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
        log.info("Order status synced from delivery event → orderId: {}, status: {}", orderId, newStatus);
    }

    private Optional<OrderStatus> mapDeliveryStatus(String deliveryStatus) {
        if (deliveryStatus == null) {
            return Optional.empty();
        }

        return switch (deliveryStatus) {
            case "ASSIGNED" -> Optional.of(OrderStatus.ASSIGNED);
            case "OUT_FOR_DELIVERY" -> Optional.of(OrderStatus.OUT_FOR_DELIVERY);
            case "DELIVERED" -> Optional.of(OrderStatus.DELIVERED);
            case "FAILED" -> Optional.of(OrderStatus.FAILED);
            default -> Optional.empty();
        };
    }

    private boolean shouldIgnoreDeliveryTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return true;
        }
        if (currentStatus == OrderStatus.DELIVERED
                || currentStatus == OrderStatus.FAILED
                || currentStatus == OrderStatus.CANCELLED) {
            return true;
        }

        return STATUS_PROGRESS.getOrDefault(newStatus, 0) < STATUS_PROGRESS.getOrDefault(currentStatus, 0);
    }

    private void assertOrderBelongsToCurrentUser(Order order) {
        Long userId = getCurrentUserId();
        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only access your own order");
        }
    }

    private void releaseStockAfterCheckoutFailure(Long orderId, RuntimeException checkoutFailure) {
        try {
            productHelperService.releaseStock(orderId);
        } catch (RuntimeException releaseFailure) {
            checkoutFailure.addSuppressed(releaseFailure);
            log.error("Failed to release reserved stock after checkout failure for orderId: {}",
                    orderId, releaseFailure);
        }
    }
}
