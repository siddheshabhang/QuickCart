package com.siddhesh.QuickCart.Dto;

import lombok.Data;

@Data
public class AddToCartReq {
    private Long productId;
    private Integer quantity;
}
