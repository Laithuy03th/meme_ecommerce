package com.example.MyWeb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchConstraints {
    private String keyword;
    private String categorySlug;
    private String brand;
    private Double minPrice;
    private Double maxPrice;
    private Double minRating;
    private String sortBy; // topRated, bestSelling, priceAsc, priceDesc, newest
    private Boolean isFollowUp;
}