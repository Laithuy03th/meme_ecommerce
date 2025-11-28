package com.example.MyWeb.dto.product;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {

    private String sku;

    @NotNull(message = "Color is required")
    private String color;

    @NotNull(message = "Size is required")
    private String size;

    private Double price; // null = use basePrice

    @NotNull(message = "Stock is required")
    private Integer stock;

    private String status; // ACTIVE / INACTIVE
}
