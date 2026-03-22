package com.siddhesh.QuickCart.Controller;

import com.siddhesh.QuickCart.Dto.ApiResponse;
import com.siddhesh.QuickCart.Dto.ProductRequestDto;
import com.siddhesh.QuickCart.Dto.ProductResponseDto;
import com.siddhesh.QuickCart.Service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ApiResponse<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto requestDto) {
        ProductResponseDto productResponseDto = productService.createProduct(requestDto);
        return new ApiResponse<>(
                true,
                "Product Successfully Created",
                productResponseDto
        );
    }

    @GetMapping
    public ApiResponse<List<ProductResponseDto>> getAllProducts() {
        return new ApiResponse<>(
                true,
                "All products fetched successfully!",
                productService.getAllProducts()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponseDto> getProductById(@PathVariable Long id) {
        return new ApiResponse<>(
                true,
                "Product fetched successfully!",
                productService.getProductById(id)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponseDto> updateProductById(@PathVariable Long id, @Valid @RequestBody ProductRequestDto requestDto) {
        return new ApiResponse<>(
                true,
                "Product details updated successfully",
                productService.updateById(id, requestDto)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteById(@PathVariable Long id) {
        productService.deleteById(id);
        return new ApiResponse<>(true, "Product deleted", null);
    }
}
