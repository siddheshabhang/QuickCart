package com.siddhesh.QuickCart.Specification;

import com.siddhesh.QuickCart.Entity.Product;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {
    public static Specification<Product> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<Product> hasMinPrice(Double minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    public static Specification<Product> hasMaxPrice(Double maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }
}