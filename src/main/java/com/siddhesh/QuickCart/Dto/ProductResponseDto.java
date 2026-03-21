package com.siddhesh.QuickCart.Dto;

public class ProductResponseDto {
    private Long id;
    private String name;
    private double price;

    public ProductResponseDto(Long id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public double getPrice() {
        return price;
    }
}
