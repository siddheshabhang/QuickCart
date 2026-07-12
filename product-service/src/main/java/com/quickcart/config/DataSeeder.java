package com.quickcart.config;

import com.quickcart.entity.Inventory;
import com.quickcart.entity.Product;
import com.quickcart.entity.Store;
import com.quickcart.repository.InventoryRepository;
import com.quickcart.repository.ProductRepository;
import com.quickcart.repository.StoreRepository;
import com.quickcart.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

/**
 * Runs once on startup after the application is fully ready.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li>Seeds 22 dark stores across India into the {@code dark_stores} DB table
 *       (idempotent — skipped if dark stores already exist).</li>
 *   <li>Loads all active dark stores into Redis GEO set {@code stores:locations}
 *       for sub-millisecond nearest-store lookups.</li>
 *   <li>Distributes all products across every active dark store: for each
 *       (dark store, product) pair that has no inventory record, creates a row
 *       with quantity = 100 so the full catalog is available everywhere.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StringRedisTemplate redisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        seedProducts();
        seedStores();
        seedRedisGeo();
        migrateProductInventory();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 0. Product Seed
    // ─────────────────────────────────────────────────────────────────────────

    private void seedProducts() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = new ClassPathResource("products.json").getInputStream();
            List<Product> products = mapper.readValue(is, new TypeReference<List<Product>>() {});
            
            if (productRepository.count() == 0) {
                productRepository.saveAll(products);
                log.info("DataSeeder: seeded {} products from JSON.", products.size());
            } else {
                List<Product> existingProducts = productRepository.findAll();
                for (Product existing : existingProducts) {
                    products.stream()
                        .filter(p -> p.getName().equals(existing.getName()))
                        .findFirst()
                        .ifPresent(p -> {
                            if (p.getImageUrl() != null) {
                                existing.setImageUrl(p.getImageUrl());
                            }
                        });
                }
                productRepository.saveAll(existingProducts);
                log.info("DataSeeder: updated existing products with images from JSON.");
            }
        } catch (Exception e) {
            log.error("DataSeeder: failed to seed products from JSON", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. DB Store Seed
    // ─────────────────────────────────────────────────────────────────────────

    private void seedStores() {
        if (storeRepository.existsBy()) {
            log.info("DataSeeder: dark_stores table already populated — skipping store seed.");
            return;
        }

        List<Store> stores = List.of(
                // ── Bengaluru (7) ─────────────────────────────────────────────
                store("QuickCart Dark Store — Neeladri",       "Bengaluru", 12.8456, 77.6603, 4.0),
                store("QuickCart Dark Store — EC Phase 1",     "Bengaluru", 12.8458, 77.6692, 3.5),
                store("QuickCart Dark Store — EC Phase 2",     "Bengaluru", 12.8368, 77.6754, 3.5),
                store("QuickCart Dark Store — Koramangala",    "Bengaluru", 12.9279, 77.6271, 4.0),
                store("QuickCart Dark Store — HSR Layout",     "Bengaluru", 12.9121, 77.6446, 4.0),
                store("QuickCart Dark Store — Whitefield",     "Bengaluru", 12.9698, 77.7500, 5.0),
                store("QuickCart Dark Store — Indiranagar",    "Bengaluru", 12.9784, 77.6408, 3.5),
                // ── Mumbai (3) ────────────────────────────────────────────────
                store("QuickCart Dark Store — Andheri",        "Mumbai",    19.1197, 72.8464, 4.0),
                store("QuickCart Dark Store — Bandra",         "Mumbai",    19.0596, 72.8656, 3.5),
                store("QuickCart Dark Store — Thane",          "Mumbai",    19.2183, 72.9781, 5.0),
                // ── Delhi NCR (3) ─────────────────────────────────────────────
                store("QuickCart Dark Store — Connaught Place","Delhi",     28.6315, 77.2167, 3.0),
                store("QuickCart Dark Store — Dwarka",         "Delhi",     28.5921, 77.0460, 5.0),
                store("QuickCart Dark Store — Noida",          "Delhi NCR", 28.5706, 77.3219, 4.5),
                // ── Hyderabad (2) ─────────────────────────────────────────────
                store("QuickCart Dark Store — Hitech City",    "Hyderabad", 17.4435, 78.3772, 4.0),
                store("QuickCart Dark Store — Gachibowli",     "Hyderabad", 17.4401, 78.3489, 4.0),
                // ── Chennai (2) ───────────────────────────────────────────────
                store("QuickCart Dark Store — T. Nagar",       "Chennai",   13.0418, 80.2341, 3.5),
                store("QuickCart Dark Store — OMR",            "Chennai",   12.9010, 80.2279, 5.0),
                // ── Pune (2) ──────────────────────────────────────────────────
                store("QuickCart Dark Store — Hinjewadi",      "Pune",      18.5912, 73.7389, 5.0),
                store("QuickCart Dark Store — Kothrud",        "Pune",      18.5074, 73.8077, 4.0),
                // ── Other cities (3) ──────────────────────────────────────────
                store("QuickCart Dark Store — Salt Lake",      "Kolkata",   22.5726, 88.4312, 5.0),
                store("QuickCart Dark Store — SG Highway",     "Ahmedabad", 23.0395, 72.5090, 5.0),
                store("QuickCart Dark Store — Malviya Nagar",  "Jaipur",    26.8467, 75.8077, 5.0)
        );

        storeRepository.saveAll(stores);
        log.info("DataSeeder: seeded {} dark stores.", stores.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Redis GEO Seed
    // ─────────────────────────────────────────────────────────────────────────

    private void seedRedisGeo() {
        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
        // Clear existing entries to ensure a clean reload on every restart
        redisTemplate.delete(StoreService.GEO_KEY);

        List<Store> activeStores = storeRepository.findByActiveTrue();
        for (Store store : activeStores) {
            // Redis GEO takes (longitude, latitude) — note the order
            geoOps.add(StoreService.GEO_KEY,
                    new Point(store.getLongitude(), store.getLatitude()),
                    "store:" + store.getId());
        }
        log.info("DataSeeder: loaded {} stores into Redis GEO set '{}'.",
                activeStores.size(), StoreService.GEO_KEY);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Inventory Distribution — all products across all dark stores
    // ─────────────────────────────────────────────────────────────────────────

    private void migrateProductInventory() {
        List<Store> activeStores = storeRepository.findByActiveTrue();
        List<Product> products   = productRepository.findAll();
        int seeded = 0;

        for (Store store : activeStores) {
            for (Product product : products) {
                boolean exists = inventoryRepository
                        .findByStoreIdAndProductId(store.getId(), product.getId())
                        .isPresent();
                if (!exists) {
                    int qty = ThreadLocalRandom.current().nextInt(20, 201); // 20–200 units
                    inventoryRepository.save(Inventory.builder()
                            .storeId(store.getId())
                            .productId(product.getId())
                            .quantity(qty)
                            .build());
                    seeded++;
                }
            }
        }

        if (seeded > 0) {
            log.info("DataSeeder: seeded {} inventory rows across {} dark stores ({} products each).",
                    seeded, activeStores.size(), products.size());
        } else {
            log.info("DataSeeder: inventory already fully distributed — no seeding needed.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private Store store(String name, String city, double lat, double lng, double radiusKm) {
        return Store.builder()
                .name(name)
                .city(city)
                .latitude(lat)
                .longitude(lng)
                .serviceRadiusKm(radiusKm)
                .active(true)
                .build();
    }
}
