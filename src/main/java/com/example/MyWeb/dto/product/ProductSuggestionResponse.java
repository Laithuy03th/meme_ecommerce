package com.example.MyWeb.dto.product;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSuggestionResponse {
    private Long id;
    private String name;
    private String slug;
    private String thumbnailUrl;
    private Double price;
    private String categoryName;
}
