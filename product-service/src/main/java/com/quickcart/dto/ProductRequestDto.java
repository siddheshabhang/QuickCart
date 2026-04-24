package com.quickcart.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDto {
    @NotBlank(message = "Product name cannot be empty!")
    private String name;

    @Min(value = 1, message = "Price must be greater than 0")
    private double price;

    private String description;

    @NotNull(message = "Stock is required")
    private Integer stock;
}
