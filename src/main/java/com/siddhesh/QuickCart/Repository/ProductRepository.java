package com.siddhesh.QuickCart.Repository;

import com.siddhesh.QuickCart.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
