package com.siddhesh.QuickCart.Service;

import com.siddhesh.QuickCart.Dto.ProductRequestDto;
import com.siddhesh.QuickCart.Dto.ProductResponseDto;
import com.siddhesh.QuickCart.Entity.Product;
import com.siddhesh.QuickCart.Exception.ResourceNotFoundException;
import com.siddhesh.QuickCart.Repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(p -> new ProductResponseDto(p.getId(), p.getName(), p.getPrice(), p.getCreatedAt(), p.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        Product product = new Product(
                productRequestDto.getName(),
                productRequestDto.getPrice()
        );
        Product saved = productRepository.save(product);
        return new ProductResponseDto(
                saved.getId(),
                saved.getName(),
                saved.getPrice(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with Id " + id)
                );
        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public ProductResponseDto updateById(Long id, ProductRequestDto requestDto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with Id " + id)
        );

        product.setName(requestDto.getName());
        product.setPrice(requestDto.getPrice());

        Product updated = productRepository.save(product);
        return new ProductResponseDto(updated.getId(),
                updated.getName(),
                updated.getPrice(),
                updated.getCreatedAt(),
                updated.getUpdatedAt());
    }
    public void deleteById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with Id " + id)
                );
        productRepository.deleteById(product.getId());
    }
}
