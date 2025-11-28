package com.example.MyWeb.dto.product;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchKeywordSuggestion {
    private String keyword;
    private String category;
    private String categorySlug;
}
