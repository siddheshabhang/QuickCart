package com.siddhesh.QuickCart.Service;

import com.siddhesh.QuickCart.Dto.AddToCartReq;
import com.siddhesh.QuickCart.Entity.Cart;
import com.siddhesh.QuickCart.Entity.CartItem;
import com.siddhesh.QuickCart.Entity.Product;
import com.siddhesh.QuickCart.Entity.User;
import com.siddhesh.QuickCart.Repository.CartItemRepository;
import com.siddhesh.QuickCart.Repository.CartRepository;
import com.siddhesh.QuickCart.Repository.ProductRepository;
import com.siddhesh.QuickCart.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void addToCart(AddToCartReq cartReq) {
        User user = getCurrentUser();
        Product product = productRepository.findById(cartReq.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found!"));

        if(cartReq.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be positive");
        }

        if(cartReq.getQuantity() > product.getStock()) {
            throw new RuntimeException("Not enough stock!");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });

        CartItem exisitingItem = (cart.getItems() == null) ? null :
                cart.getItems().stream()
                        .filter(item -> item.getProduct().getId().equals(product.getId()))
                        .findFirst()
                        .orElse(null);

        if(exisitingItem != null) {
            int newQuantity = exisitingItem.getQuantity() + cartReq.getQuantity();
            if(newQuantity > product.getStock()) {
                throw new RuntimeException("Exceeds available stock");
            }
            exisitingItem.setQuantity(newQuantity);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(cartReq.getQuantity())
                    .build();
            cartItemRepository.save(item);
        }
    }

    @Transactional
    public void removeFromCart(Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart Item not found!"));

        User user = getCurrentUser();

        if(!item.getCart().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized!");
        }
        cartItemRepository.delete(item);
    }
}
