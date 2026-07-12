package com.quickcart.service;

import com.quickcart.common.exception.ResourceNotFoundException;
import com.quickcart.dto.ProductRequestDto;
import com.quickcart.common.dto.ProductResponseDto;
import com.quickcart.entity.Inventory;
import com.quickcart.entity.Product;
import com.quickcart.entity.Store;
import com.quickcart.mapper.ProductMapper;
import com.quickcart.repository.InventoryRepository;
import com.quickcart.repository.ProductRepository;
import com.quickcart.repository.StoreRepository;
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
    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;
    private final ProductMapper productMapper;

    /**
     * Returns all products available at a specific dark store.
     *
     * <p>Only includes products with quantity > 0 in the store's inventory.
     * The {@code stock} field in the response reflects this store's quantity.
     *
     * @param storeId the dark store assigned to the customer (from X-Store-Id header)
     */
    public List<ProductResponseDto> getAllProducts(Long storeId) {
        if (storeId == null) {
            // Admin/internal call with no store context — return all products
            return productRepository.findAll()
                    .stream()
                    .map(productMapper::toDto)
                    .toList();
        }

        return inventoryRepository
                .findByStoreIdAndQuantityGreaterThan(storeId, 0)
                .stream()
                .map(inv -> {
                    Product product = productRepository.findById(inv.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Product not found: " + inv.getProductId()));
                    return productMapper.toDto(product, inv.getQuantity());
                })
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
                .build();
        Product saved = productRepository.save(product);

        // Seed an Inventory row for every active dark store so the product
        // is immediately visible in the catalogue.
        // Without this, customers would see no inventory for the new product.
        List<Store> activeStores = storeRepository.findByActiveTrue();
        for (Store store : activeStores) {
            inventoryRepository.save(Inventory.builder()
                    .storeId(store.getId())
                    .productId(saved.getId())
                    .quantity(productRequestDto.getStock())
                    .build());
        }

        return productMapper.toDto(saved, productRequestDto.getStock());
    }

    public ProductResponseDto getProductById(Long id, Long storeId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with Id " + id)
                );
        
        if (storeId != null) {
            return inventoryRepository.findByStoreIdAndProductId(storeId, id)
                    .map(inv -> productMapper.toDto(product, inv.getQuantity()))
                    .orElseGet(() -> productMapper.toDto(product, 0)); // No inventory for store
        }
        
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
        productRepository.save(product);

        // Upsert inventory across all active stores so the stock value
        // submitted by the store owner is actually persisted.
        // Previously this was validated but silently discarded.
        List<Store> activeStores = storeRepository.findByActiveTrue();
        for (Store store : activeStores) {
            Inventory inv = inventoryRepository
                    .findByStoreIdAndProductId(store.getId(), id)
                    .orElseGet(() -> Inventory.builder()
                            .storeId(store.getId())
                            .productId(id)
                            .build());
            inv.setQuantity(requestDto.getStock());
            inventoryRepository.save(inv);
        }

        return productMapper.toDto(product, requestDto.getStock());
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

    public List<ProductResponseDto> searchProducts(String name, double minPrice, double maxPrice, Long storeId) {
        if (storeId == null) {
            return productRepository
                    .findByNameContainingIgnoreCaseAndPriceBetween(name, minPrice, maxPrice)
                    .stream()
                    .map(productMapper::toDto)
                    .toList();
        } else {
            return inventoryRepository
                    .searchAvailableProductsInStore(storeId, name, minPrice, maxPrice)
                    .stream()
                    .map(inv -> productMapper.toDto(inv.getProduct(), inv.getQuantity()))
                    .toList();
        }
    }

    public List<ProductResponseDto> filterProducts(String name, Double minPrice, Double maxPrice, Long storeId) {
        if (storeId == null) {
            Specification<Product> spec = Specification
                    .where(hasName(name))
                    .and(hasMinPrice(minPrice))
                    .and(hasMaxPrice(maxPrice));
            return productRepository.findAll(spec)
                    .stream()
                    .map(productMapper::toDto)
                    .toList();
        } else {
            return inventoryRepository
                    .filterAvailableProductsInStore(storeId, name, minPrice, maxPrice)
                    .stream()
                    .map(inv -> productMapper.toDto(inv.getProduct(), inv.getQuantity()))
                    .toList();
        }
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

}
