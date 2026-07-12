package com.quickcart.dto;

import lombok.Data;

@Data
public class OrderRequestDto {
    private String address;
    private String phoneNumber;
    /** The dark store the customer is shopping from (returned by GET /stores/nearest). */
    private Long storeId;
}
