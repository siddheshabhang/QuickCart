package com.quickcart.service;

import com.quickcart.common.dto.ProductResponseDto;
import com.quickcart.common.exception.ResourceNotFoundException;
import com.quickcart.dto.AddToCartReq;
import com.quickcart.dto.CartItemDto;
import com.quickcart.dto.CartResponseDto;
import com.quickcart.entity.Cart;
import com.quickcart.entity.CartItem;
import com.quickcart.feign.ProductClient;
import com.quickcart.repository.CartItemRepository;
import com.quickcart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductHelperService productHelperService;

    private Long getCurrentUserId() {
        String principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        try {
            return Long.parseLong(principal);
        } catch (NumberFormatException ex) {
            return (long) principal.hashCode() & 0xFFFFFFFFL;
        }
    }

    @Transactional
    public void addToCart(AddToCartReq cartReq, Long storeId) {
        Long userId = getCurrentUserId();

        // Validate product via Feign — product-service is the source of truth
        ProductResponseDto product = productHelperService.getProductById(cartReq.getProductId(), storeId).getData();

        if (cartReq.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (!product.isAvailable()) {
            throw new IllegalArgumentException("Product is out of stock");
        }

        if (cartReq.getQuantity() > product.getStock()) {
            throw new IllegalArgumentException("Not enough stock!");
        }

        // Find or create the cart for this user
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().userId(userId).build()
                ));

        // Query by cart + product so we reliably merge duplicate adds.
        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + cartReq.getQuantity();
            if (newQuantity > product.getStock()) {
                throw new IllegalArgumentException("Exceeds available stock");
            }
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            cartItemRepository.save(CartItem.builder()
                    .cart(cart)
                    .productId(product.getId())
                    .quantity(cartReq.getQuantity())
                    .build());
        }
    }

    @Transactional
    public void updateQuantity(Long cartItemId, int quantity, Long storeId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        Long userId = getCurrentUserId();
        if (!item.getCart().getUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to update this item");
        }

        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        if (quantity == 0) {
            cartItemRepository.delete(item);
            return;
        }

        ProductResponseDto product = productHelperService.getProductById(item.getProductId(), storeId).getData();
        if (quantity > product.getStock()) {
            throw new IllegalArgumentException("Exceeds available stock");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    @Transactional
    public void removeFromCart(Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        Long userId = getCurrentUserId();

        if (!item.getCart().getUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to remove this item");
        }
        cartItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public CartResponseDto getCart(Long storeId) {
        Long userId = getCurrentUserId();

        // If user has never added to cart, return empty cart instead of 500
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return CartResponseDto.builder()
                    .items(new ArrayList<>())
                    .totalAmount(0.0)
                    .build();
        }

        List<CartItemDto> items = new ArrayList<>();
        double total = 0;

        List<CartItem> cartItems = cart.getItems() != null ? cart.getItems() : java.util.Collections.emptyList();

        for (CartItem item : cartItems) {
            // Enrich each cart item with live product data from product-service
            ProductResponseDto product = productHelperService.getProductById(item.getProductId(), storeId).getData();
            double itemTotal = product.getPrice() * item.getQuantity();
            total += itemTotal;

            items.add(CartItemDto.builder()
                    .cartItemId(item.getId())
                    .productId(item.getProductId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(item.getQuantity())
                    .itemTotal(itemTotal)
                    .build());
        }

        return CartResponseDto.builder()
                .items(items)
                .totalAmount(total)
                .build();
    }

    @Transactional
    public void clearCart() {
        Long userId = getCurrentUserId();
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
