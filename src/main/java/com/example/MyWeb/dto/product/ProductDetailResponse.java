package com.example.MyWeb.dto.product;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailResponse {

    private Long id;
    private String name;
    private String slug;

    private String shortDesc;
    private String longDesc;

    private String categorySlug;
    private String categoryName;

    private Double basePrice;
    private String thumbnailUrl;

    private String status; // ACTIVE / INACTIVE / DRAFT

    // Product metadata
    private String brand;
    private String sku;
    private Double weight; // kg - for shipping calculation

    // Stock information
    private Integer stockQuantity;
    private String stockStatus; // "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"

    private Double averageRating;
    private Integer reviewCount;
    private Integer soldCount;
    private Integer viewCount;

    // Media
    private String videoUrl;
    private Boolean isFeatured;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ProductImageResponse> images;
    private List<ProductVariantResponse> variants;

    // Thông số kỹ thuật theo category
    private Map<String, String> specifications;
}
