package com.siddhesh.QuickCart.Controller;

import com.siddhesh.QuickCart.Dto.AddToCartReq;
import com.siddhesh.QuickCart.Dto.ApiResponse;
import com.siddhesh.QuickCart.Dto.CartResponseDto;
import com.siddhesh.QuickCart.Repository.CartRepository;
import com.siddhesh.QuickCart.Service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final CartRepository cartRepository;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addToCart(@RequestBody AddToCartReq cartReq) {
        cartService.addToCart(cartReq);
        return ResponseEntity.ok(new ApiResponse<>(true, "Added to cart", null));
    }

    @DeleteMapping("remove/{id}")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(@PathVariable Long id) {
        cartService.removeFromCart(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Removed from cart", null));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CartResponseDto>> getCart() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Cart fetched", cartService.getCart()));
    }
}
