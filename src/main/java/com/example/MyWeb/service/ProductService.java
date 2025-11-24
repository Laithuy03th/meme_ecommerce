package com.example.MyWeb.service;

import com.example.MyWeb.dto.product.ProductDetailResponse;
import com.example.MyWeb.dto.product.ProductListItemResponse;
import org.springframework.data.domain.Page;

public interface ProductService {

    Page<ProductListItemResponse> getProducts(
            String keyword,
            String categorySlug,
            int page,
            int size,
            String sortBy // newest, oldest, priceAsc, priceDesc
    );

    ProductDetailResponse getProductDetailBySlug(String slug);

    ProductDetailResponse getProductDetailById(Long id);
}
