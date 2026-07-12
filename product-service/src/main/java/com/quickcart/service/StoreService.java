package com.quickcart.service;

import com.quickcart.common.exception.ResourceNotFoundException;
import com.quickcart.dto.StoreResponseDto;
import com.quickcart.entity.Store;
import com.quickcart.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

    /** Redis key that holds the GEO set of all active dark stores. */
    public static final String GEO_KEY = "stores:locations";

    /**
     * Maximum radius for the nearest-store search.
     * Standard quick-commerce: 8 km. No store within 8 km → not serviceable.
     */
    private static final double MAX_SEARCH_RADIUS_KM = 8.0;

    private final StringRedisTemplate redisTemplate;
    private final StoreRepository storeRepository;

    /**
     * Finds the nearest active dark store to the given GPS coordinates.
     *
     * Uses Redis GEOSEARCH (O(N+log(M))) for sub-millisecond lookup.
     * Falls back to "not deliverable" response if no store within 8 km.
     *
     * @param lat customer's latitude
     * @param lng customer's longitude
     */
    public StoreResponseDto findNearest(double lat, double lng) {
        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();

        // Redis GEO takes (longitude, latitude) — note the order
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = geoOps.radius(
                GEO_KEY,
                new Circle(
                        new Point(lng, lat),
                        new Distance(MAX_SEARCH_RADIUS_KM, Metrics.KILOMETERS)
                ),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(1)
        );

        if (results == null || results.getContent().isEmpty()) {
            log.info("No store found within {}km for lat={}, lng={}", MAX_SEARCH_RADIUS_KM, lat, lng);
            return StoreResponseDto.builder()
                    .deliverable(false)
                    .message("We are not in your area yet. Stay tuned — we're expanding!")
                    .build();
        }

        var result = results.getContent().get(0);
        String memberKey = result.getContent().getName(); // "store:3"
        double distanceKm = result.getDistance().getValue();

        // Parse storeId from "store:3"
        Long storeId = Long.parseLong(memberKey.split(":")[1]);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));

        // Double-check active flag (Redis may have stale entries briefly after deactivation)
        if (!store.getActive()) {
            log.warn("Redis returned an inactive store (id={}) — returning not serviceable", storeId);
            return StoreResponseDto.builder()
                    .deliverable(false)
                    .message("Store temporarily unavailable. Please try again soon.")
                    .build();
        }

        double roundedDistance = BigDecimal.valueOf(distanceKm)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        log.info("Nearest store for lat={}, lng={}: {} ({}km)", lat, lng, store.getName(), roundedDistance);

        return StoreResponseDto.builder()
                .storeId(store.getId())
                .storeName(store.getName())
                .city(store.getCity())
                .distanceKm(roundedDistance)
                .deliverable(true)
                .build();
    }
}
