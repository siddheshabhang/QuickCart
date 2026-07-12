package com.quickcart.mapper;

import com.quickcart.common.dto.ProductResponseDto;
import com.quickcart.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    /**
     * Maps a Product entity to a response DTO.
     * Stock ({@code stock} field) is intentionally NOT set here — it is
     * store-specific and must be populated by the caller from the Inventory table.
     */
    public ProductResponseDto toDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .stock(null)        // set by ProductService from Inventory for the given storeId
                .available(false)   // set by ProductService when inventory.quantity > 0
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * Maps a Product + store-specific inventory quantity to a response DTO.
     *
     * @param product       the product entity
     * @param storeQuantity available quantity at the customer's dark store
     */
    public ProductResponseDto toDto(Product product, Integer storeQuantity) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .stock(storeQuantity)
                .available(storeQuantity != null && storeQuantity > 0)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
