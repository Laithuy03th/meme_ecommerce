package com.example.MyWeb.service;

import com.example.MyWeb.dto.product.ProductVariantRequest;
import com.example.MyWeb.dto.product.ProductVariantResponse;
import java.util.List;

public interface AdminProductVariantService {
    List<ProductVariantResponse> getVariantsByProductId(Long productId);

    ProductVariantResponse getVariantById(Long variantId);

    ProductVariantResponse createVariant(Long productId, ProductVariantRequest request);

    ProductVariantResponse updateVariant(Long variantId, ProductVariantRequest request);

    void deleteVariant(Long variantId);
}
