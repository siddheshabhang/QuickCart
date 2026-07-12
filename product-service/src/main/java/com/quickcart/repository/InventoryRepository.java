package com.quickcart.repository;

import com.quickcart.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /** Stock lookup for a specific (store, product) pair — used by reserve/release. */
    Optional<Inventory> findByStoreIdAndProductId(Long storeId, Long productId);

    /** Locks the row while stock is being reserved or restored. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.storeId = :storeId and i.productId = :productId")
    Optional<Inventory> findByStoreIdAndProductIdForUpdate(
            @Param("storeId") Long storeId,
            @Param("productId") Long productId);

    /** Returns all in-stock products at a store — used by product catalog filtering. */
    List<Inventory> findByStoreIdAndQuantityGreaterThan(Long storeId, int minQuantity);

    /** Used by DataSeeder to check if inventory exists for a product. */
    boolean existsByProductId(Long productId);

    @Query("SELECT i FROM Inventory i WHERE i.storeId = :storeId AND i.quantity > 0 " +
           "AND LOWER(i.product.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND i.product.price BETWEEN :minPrice AND :maxPrice")
    List<Inventory> searchAvailableProductsInStore(
            @Param("storeId") Long storeId,
            @Param("name") String name,
            @Param("minPrice") double minPrice,
            @Param("maxPrice") double maxPrice
    );

    @Query("SELECT i FROM Inventory i WHERE i.storeId = :storeId AND i.quantity > 0 " +
           "AND (:name IS NULL OR LOWER(i.product.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:minPrice IS NULL OR i.product.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR i.product.price <= :maxPrice)")
    List<Inventory> filterAvailableProductsInStore(
            @Param("storeId") Long storeId,
            @Param("name") String name,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );
}
