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
    private String thumbnailUrl;
    private String status;
}
