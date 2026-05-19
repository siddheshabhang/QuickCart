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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private static final String ORDER_SERVICE = "ORDER-SERVICE";

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

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE')")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAllProducts() {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "All products fetched successfully!",
                productService.getAllProducts()
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Product fetched successfully!",
                productService.getProductById(id)
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
            @RequestParam(name = "maxPrice") double maxPrice) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Filtered products fetched",
                productService.searchProducts(name, minPrice, maxPrice)
        ));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE')")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> filterProducts(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "minPrice", required = false) Double minPrice,
            @RequestParam(name = "maxPrice", required = false) Double maxPrice) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Filtered products fetched",
                productService.filterProducts(name, minPrice, maxPrice)
        ));
    }

    @PutMapping("/{id}/deduct-stock")
    public ResponseEntity<ApiResponse<Void>> deductStock(
            @PathVariable("id") Long id,
            @RequestParam("quantity") int quantity,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalService) {
        if (!ORDER_SERVICE.equals(internalService)) {
            throw new AccessDeniedException("Stock can only be deducted by order-service");
        }
        productService.deductStock(id, quantity);
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock deducted", null));
    }
}
