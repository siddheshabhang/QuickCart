package com.quickcart.repository;

import com.quickcart.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    /** Returns all active stores — used by DataSeeder to seed Redis GEO. */
    List<Store> findByActiveTrue();

    /** Checks if any store exists — used by DataSeeder to avoid re-seeding. */
    boolean existsBy();
}
