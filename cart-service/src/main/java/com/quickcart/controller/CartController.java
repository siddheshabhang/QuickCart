package com.quickcart.controller;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.dto.AddToCartReq;
import com.quickcart.dto.CartResponseDto;
import com.quickcart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> addToCart(
            @Valid @RequestBody AddToCartReq cartReq,
            @RequestHeader(value = "X-Store-Id", required = false) Long storeId) {
        cartService.addToCart(cartReq, storeId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Added to cart", null));
    }

    @DeleteMapping("/remove/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(@PathVariable("id") Long id) {
        cartService.removeFromCart(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Removed from cart", null));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> updateQuantity(
            @PathVariable("id") Long id,
            @RequestParam("quantity") int quantity,
            @RequestHeader(value = "X-Store-Id", required = false) Long storeId) {
        cartService.updateQuantity(id, quantity, storeId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cart item quantity updated", null));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CartResponseDto>> getCart(
            @RequestHeader(value = "X-Store-Id", required = false) Long storeId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Cart fetched", cartService.getCart(storeId)));
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok(new ApiResponse<>(true, "Cart cleared", null));
    }
}
