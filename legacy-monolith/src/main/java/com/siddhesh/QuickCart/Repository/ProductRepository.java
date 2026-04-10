package com.siddhesh.QuickCart.Repository;

import com.siddhesh.QuickCart.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {
    List<Product> findByNameContainingIgnoreCaseAndPriceBetween(
            String name,
            double minPrice,
            double maxPrice
    );
}
