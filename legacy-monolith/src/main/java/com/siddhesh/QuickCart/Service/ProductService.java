package com.siddhesh.QuickCart.Service;

import com.siddhesh.QuickCart.Dto.ProductRequestDto;
import com.siddhesh.QuickCart.Dto.ProductResponseDto;
import com.siddhesh.QuickCart.Entity.Product;
import com.siddhesh.QuickCart.Exception.ResourceNotFoundException;
import com.siddhesh.QuickCart.Mapper.ProductMapper;
import com.siddhesh.QuickCart.Repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.siddhesh.QuickCart.Specification.ProductSpecification.*;

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
        if(productRequestDto.getStock() < 0) {
            throw new RuntimeException("Stock cannot be negative");
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

        if(product.getStock() < 0) {
            throw new RuntimeException("Stock cannot be negative");
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
        Page<Product> productPage = productRepository.findAll(pageable);
        return productPage.map(productMapper::toDto);
    }

    public List<ProductResponseDto> searchProducts(String name, double minPrice, double maxPrice) {
        List<Product> products = productRepository.findByNameContainingIgnoreCaseAndPriceBetween(
                name, minPrice, maxPrice);
        return products.stream()
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
}