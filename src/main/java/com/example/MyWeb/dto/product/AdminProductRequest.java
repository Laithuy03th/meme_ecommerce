// src/main/java/com/example/MyWeb/dto/product/AdminProductRequest.java
package com.example.MyWeb.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminProductRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String slug; // dùng cho URL

    private String shortDesc;
    private String longDesc;

    @NotNull
    private Long categoryId;

    @NotNull
    @Min(0)
    private Double basePrice;

    @Min(0)
    private Integer stockQuantity;

    private String thumbnailUrl;
    private java.util.List<String> imageUrls; // Additional images

    // New standard e-commerce fields
    private String brand;
    private String sku;
    private Double weight;
    private Boolean isFeatured;
    private String videoUrl;

    // ACTIVE / INACTIVE / DRAFT
    private String status;

    // Thông số kỹ thuật theo category (key-value pairs)
    private java.util.Map<String, String> specifications;
}
