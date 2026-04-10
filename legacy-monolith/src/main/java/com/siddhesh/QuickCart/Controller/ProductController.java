package com.siddhesh.QuickCart.Controller;

import com.siddhesh.QuickCart.Dto.ApiResponse;
import com.siddhesh.QuickCart.Dto.ProductRequestDto;
import com.siddhesh.QuickCart.Dto.ProductResponseDto;
import com.siddhesh.QuickCart.Service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STORE')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(@Valid @RequestBody ProductRequestDto requestDto) {
        ProductResponseDto productResponseDto = productService.createProduct(requestDto);
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Product Successfully Created",
                productResponseDto
        ), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','STORE')")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAllProducts() {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "All products fetched successfully!",
                productService.getAllProducts()
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','STORE')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Product fetched successfully!",
                productService.getProductById(id)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STORE')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProductById(@PathVariable Long id, @Valid @RequestBody ProductRequestDto requestDto) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Product details updated successfully",
                productService.updateById(id, requestDto)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STORE')")
    public ResponseEntity<ApiResponse<String>> deleteById(@PathVariable Long id) {
        productService.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product deleted", null));
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('CUSTOMER','STORE')")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getPaginatedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "price") String sortby) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Paginated products fetched",
                productService.getProductsPaginated(page, size, sortby)
        ));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CUSTOMER','STORE')")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> searchProducts(
            @RequestParam String name,
            @RequestParam double minPrice,
            @RequestParam double maxPrice) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Filtered products fetched",
                productService.searchProducts(name, minPrice, maxPrice)
        ));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('CUSTOMER','STORE')")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> filterProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Filtered products fetched",
                productService.filterProducts(name, minPrice, maxPrice)
        ));
    }
}