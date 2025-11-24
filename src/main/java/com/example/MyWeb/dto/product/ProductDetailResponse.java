package com.example.MyWeb.dto.product;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ProductImageResponse> images;
    private List<ProductVariantResponse> variants;
}
