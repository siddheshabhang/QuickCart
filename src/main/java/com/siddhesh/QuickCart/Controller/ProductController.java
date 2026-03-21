package com.siddhesh.QuickCart.Controller;

import com.siddhesh.QuickCart.Dto.ApiResponse;
import com.siddhesh.QuickCart.Dto.ProductRequestDto;
import com.siddhesh.QuickCart.Dto.ProductResponseDto;
import com.siddhesh.QuickCart.Service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/demo")
    public ApiResponse<ProductResponseDto> getDemoProduct() {
        ProductResponseDto responseDto = productService.getDemoProduct();
        return new ApiResponse<>(
                true,
                "Product fetched successfully",
                        responseDto
        );
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
}
