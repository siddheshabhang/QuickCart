package com.quickcart.controller;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.common.dto.ProductResponseDto;
import com.quickcart.dto.ProductRequestDto;
import com.quickcart.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('STORE')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
            @Valid @RequestBody ProductRequestDto requestDto) {
        ProductResponseDto productResponseDto = productService.createProduct(requestDto);
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Product Successfully Created",
                productResponseDto
        ), HttpStatus.CREATED);
    }

    /**
     * Returns products available at the customer's dark store.
     * Reads {@code X-Store-Id} header forwarded by the API Gateway.
     * If absent (admin/internal call), returns the full catalogue without stock filtering.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE')")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAllProducts(
            @RequestHeader(value = "X-Store-Id", required = false) Long storeId) {
        System.out.println("====== STORE ID RECEIVED: " + storeId + " ======");
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "All products fetched successfully!",
                productService.getAllProducts(storeId)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Store-Id", required = false) Long storeId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Product fetched successfully!",
                productService.getProductById(id, storeId)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STORE')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProductById(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductRequestDto requestDto) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Product details updated successfully",
                productService.updateById(id, requestDto)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STORE')")
    public ResponseEntity<ApiResponse<String>> deleteById(@PathVariable("id") Long id) {
        productService.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product deleted", null));
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE')")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getPaginatedProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "sortby", defaultValue = "price") String sortby) {

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Paginated products fetched",
                productService.getProductsPaginated(page, size, sortby)
        ));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE')")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> searchProducts(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "minPrice") double minPrice,
            @RequestParam(name = "maxPrice") double maxPrice,
            @RequestHeader(value = "X-Store-Id", required = false) Long storeId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Filtered products fetched",
                productService.searchProducts(name, minPrice, maxPrice, storeId)
        ));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE')")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> filterProducts(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "minPrice", required = false) Double minPrice,
            @RequestParam(name = "maxPrice", required = false) Double maxPrice,
            @RequestHeader(value = "X-Store-Id", required = false) Long storeId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Filtered products fetched",
                productService.filterProducts(name, minPrice, maxPrice, storeId)
        ));
    }
}
