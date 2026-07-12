package com.quickcart.controller;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.dto.StoreResponseDto;
import com.quickcart.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    /**
     * Returns the nearest QuickCart dark store for the given GPS coordinates.
     *
     * <p>Called by the frontend immediately after login. The returned {@code storeId}
     * is saved in local storage and sent as {@code X-Store-Id} header on every
     * subsequent API call, so the product catalog is filtered for that store.
     *
     * <p>Max search radius: 8 km (quick-commerce standard).
     * If no store is found, returns {@code deliverable: false}.
     *
     * @param lat customer latitude
     * @param lng customer longitude
     */
    @GetMapping("/nearest")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DELIVERY', 'STORE')")
    public ResponseEntity<ApiResponse<StoreResponseDto>> getNearestStore(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng) {

        StoreResponseDto result = storeService.findNearest(lat, lng);
        return ResponseEntity.ok(new ApiResponse<>(true, "Store lookup successful", result));
    }
}
