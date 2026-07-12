package com.quickcart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response returned by GET /stores/nearest.
 * When deliverable is false, storeId/storeName/distanceKm are null.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponseDto {
    private Long storeId;
    private String storeName;
    private String city;
    private Double distanceKm;
    /** true = a store covers this location and the customer can shop. */
    private Boolean deliverable;
    /** Populated only when deliverable = false. */
    private String message;
}
