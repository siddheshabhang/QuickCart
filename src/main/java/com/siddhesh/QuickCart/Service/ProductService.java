package com.siddhesh.QuickCart.Service;

import com.siddhesh.QuickCart.Dto.ProductRequestDto;
import com.siddhesh.QuickCart.Dto.ProductResponseDto;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    public ProductResponseDto getDemoProduct() {
        return new ProductResponseDto(1L, "iPhone 17 Pro", 154900);
    }

    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        return new ProductResponseDto(1L, productRequestDto.getName(), productRequestDto.getPrice());
    }
}
