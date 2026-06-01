package com.example.MyWeb.dto.product;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductListItemResponse {

    private Long id;
    private String name;
    private String slug;
    private String thumbnailUrl;
    private Double price;

    private String categorySlug;
    private String categoryName;
    private String shortDesc;

    // Enhanced fields for better UX (Shopee-level)
    private String brand;
    private Double averageRating;
    private Integer reviewCount;
    private Integer soldCount;
    private String stockStatus; // "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"
    private Boolean isFeatured;
    private Integer discountPercent; // For future flash sale feature

    private LocalDateTime createdAt;
}
