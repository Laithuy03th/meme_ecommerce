package com.example.MyWeb.service;

import com.example.MyWeb.dto.product.ProductDetailResponse;
import com.example.MyWeb.dto.product.ProductListItemResponse;
import com.example.MyWeb.dto.product.ProductSuggestionResponse;
import com.example.MyWeb.dto.product.SearchKeywordSuggestion;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {

    Page<ProductListItemResponse> getProducts(
            String keyword,
            String categorySlug,
            Double minPrice,
            Double maxPrice,
            int page,
            int size,
            String sortBy // newest, oldest, priceAsc, priceDesc
    );

    ProductDetailResponse getProductDetailBySlug(String slug);

    ProductDetailResponse getProductDetailById(Long id);

    Page<ProductListItemResponse> getRelatedProducts(Long productId, int page, int size);

    List<ProductSuggestionResponse> getSearchSuggestions(String keyword, String categorySlug, int limit);

    List<SearchKeywordSuggestion> getPopularSearchKeywords();
}
