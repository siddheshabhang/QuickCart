package com.siddhesh.QuickCart.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
public class ProductRequestDto {
    @NotBlank(message = "Product name cannot be empty!")
    public String name;

    @Min(value = 1, message = "Price must be greater than 0")
    public double price;
}
