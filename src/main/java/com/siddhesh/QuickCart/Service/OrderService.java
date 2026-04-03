package com.siddhesh.QuickCart.Service;

import com.siddhesh.QuickCart.Dto.OrderResponseDto;
import com.siddhesh.QuickCart.Entity.*;
import com.siddhesh.QuickCart.Exception.ResourceNotFoundException;
import com.siddhesh.QuickCart.Mapper.OrderMapper;
import com.siddhesh.QuickCart.Repository.CartRepository;
import com.siddhesh.QuickCart.Repository.OrderRepository;
import com.siddhesh.QuickCart.Repository.ProductRepository;
import com.siddhesh.QuickCart.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByEmail(email).orElseThrow(
                () -> new RuntimeException("User not found"));
    }

    @Transactional
    public OrderResponseDto placeOrder() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + user.getEmail()));

        if(cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0;

        //Step 1: Validate + Deduct Stock
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            product.deductStock(cartItem.getQuantity()); // validates + deducts in one call
            productRepository.save(product);

            orderItems.add(OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build());

            total += product.getPrice() * cartItem.getQuantity();
        }

        // Step 2: Create Order
        Order order = Order.builder()
                .user(user)
                .totalAmount(total)
                .items(orderItems)
                .status(OrderStatus.CREATED)
                .build();
        // Link back
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        Order savedOrder = orderRepository.save(order);

        // Step 3: Clear Cart
        cart.getItems().clear();
        cartRepository.save(cart);

        return orderMapper.toDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getUserOrders() {
        User user = getCurrentUser();
        List<Order> orders = orderRepository.findByUser(user);
        return orders.stream()
                .map(orderMapper::toDto)
                .toList();
    }

    private void validateOrderOwnership(Order order, User user) {
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to order");
        }
    }
}
