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

    private String brand;
    private Double averageRating;
    private Integer reviewCount;
    private Integer soldCount;
    private String stockStatus;
    private Boolean isFeatured;
    private Integer discountPercent;

    private LocalDateTime createdAt;
}
