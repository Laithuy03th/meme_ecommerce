// src/main/java/com/example/MyWeb/dto/product/AdminProductResponse.java
package com.example.MyWeb.dto.product;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String shortDesc;
    private String longDesc;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private Double basePrice;
    private Integer stockQuantity;
    private String thumbnailUrl;
    private java.util.List<String> imageUrls;
    
    private String brand;
    private String sku;
    private Double weight;
    private Boolean isFeatured;
    private String videoUrl;
    
    private Double averageRating;
    private Integer reviewCount;
    private Integer soldCount;
    private Integer viewCount;

    private String status;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    // Thông số kỹ thuật
    private java.util.Map<String, String> specifications;
}
