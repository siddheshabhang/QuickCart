package com.quickcart.service;

import com.quickcart.common.exception.ResourceNotFoundException;
import com.quickcart.dto.ProductRequestDto;
import com.quickcart.common.dto.ProductResponseDto;
import com.quickcart.entity.Product;
import com.quickcart.mapper.ProductMapper;
import com.quickcart.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        if (productRequestDto.getStock() < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        Product product = Product.builder()
                .name(productRequestDto.getName())
                .price(productRequestDto.getPrice())
                .description(productRequestDto.getDescription())
                .stock(productRequestDto.getStock())
                .build();
        return productMapper.toDto(productRepository.save(product));
    }

    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with Id " + id)
                );
        return productMapper.toDto(product);
    }

    @Transactional
    public ProductResponseDto updateById(Long id, ProductRequestDto requestDto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with Id " + id));

        if (requestDto.getStock() < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        product.setName(requestDto.getName());
        product.setPrice(requestDto.getPrice());
        product.setDescription(requestDto.getDescription());
        product.setStock(requestDto.getStock());

        productRepository.save(product);
        return productMapper.toDto(product);
    }

    @Transactional
    public void deleteById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with Id " + id)
                );
        productRepository.deleteById(product.getId());
    }

    public Page<ProductResponseDto> getProductsPaginated(int page, int size, String sortby) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortby).descending());
        return productRepository.findAll(pageable).map(productMapper::toDto);
    }

    public List<ProductResponseDto> searchProducts(String name, double minPrice, double maxPrice) {
        return productRepository
                .findByNameContainingIgnoreCaseAndPriceBetween(name, minPrice, maxPrice)
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    public List<ProductResponseDto> filterProducts(String name, Double minPrice, Double maxPrice) {
        Specification<Product> spec = Specification
                .where(hasName(name))
                .and(hasMinPrice(minPrice))
                .and(hasMaxPrice(maxPrice));
        return productRepository.findAll(spec)
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    // ── Specification helpers ──────────────────────────────────────────────────

    private static Specification<Product> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    private static Specification<Product> hasMinPrice(Double minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    private static Specification<Product> hasMaxPrice(Double maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    @Transactional
    public void deductStock(Long id, int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with Id " + id));
        product.deductStock(quantity);
        productRepository.save(product);
    }
}