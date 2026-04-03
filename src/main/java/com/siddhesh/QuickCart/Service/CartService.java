package com.siddhesh.QuickCart.Service;

import com.siddhesh.QuickCart.Dto.AddToCartReq;
import com.siddhesh.QuickCart.Dto.CartItemDto;
import com.siddhesh.QuickCart.Dto.CartResponseDto;
import com.siddhesh.QuickCart.Entity.Cart;
import com.siddhesh.QuickCart.Entity.CartItem;
import com.siddhesh.QuickCart.Entity.Product;
import com.siddhesh.QuickCart.Entity.User;
import com.siddhesh.QuickCart.Exception.ResourceNotFoundException;
import com.siddhesh.QuickCart.Repository.CartItemRepository;
import com.siddhesh.QuickCart.Repository.CartRepository;
import com.siddhesh.QuickCart.Repository.ProductRepository;
import com.siddhesh.QuickCart.Repository.UserRepository;
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
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public void addToCart(AddToCartReq cartReq) {
        User user = getCurrentUser();
        Product product = productRepository.findById(cartReq.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + cartReq.getProductId()));

        if (cartReq.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (!product.isAvailable()) {
            throw new IllegalArgumentException("Product is out of stock");
        }

        if (cartReq.getQuantity() > product.getStock()) {
            throw new IllegalArgumentException("Not enough stock!");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));

        CartItem existingItem = (cart.getItems() == null) ? null :
                cart.getItems().stream()
                        .filter(item -> item.getProduct().getId().equals(product.getId()))
                        .findFirst()
                        .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + cartReq.getQuantity();
            if (newQuantity > product.getStock()) {
                throw new IllegalArgumentException("Exceeds available stock");
            }
            existingItem.setQuantity(newQuantity);
        } else {
            cartItemRepository.save(CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(cartReq.getQuantity())
                    .build());
        }
    }

    @Transactional
    public void removeFromCart(Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        User user = getCurrentUser();

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to remove this item");
        }
        cartItemRepository.delete(item);
    }

    public CartResponseDto getCart() {

        User user = getCurrentUser();

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        double total = 0;

        List<CartItemDto> items = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            double itemTotal = item.getProduct().getPrice() * item.getQuantity();
            total += itemTotal;
            items.add(
                    CartItemDto.builder()
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .price(item.getProduct().getPrice())
                            .quantity(item.getQuantity())
                            .build()
            );
        }
        return CartResponseDto.builder()
                .items(items)
                .totalAmount(total)
                .build();
    }
}