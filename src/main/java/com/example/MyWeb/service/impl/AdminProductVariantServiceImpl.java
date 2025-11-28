package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.product.ProductVariantRequest;
import com.example.MyWeb.dto.product.ProductVariantResponse;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.model.ProductVariant;
import com.example.MyWeb.repository.ProductRepository;
import com.example.MyWeb.repository.ProductVariantRepository;
import com.example.MyWeb.service.AdminProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductVariantServiceImpl implements AdminProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    private ProductVariantResponse toDto(ProductVariant variant, Double basePrice) {
        Double effectivePrice = (variant.getPrice() != null) ? variant.getPrice() : basePrice;
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .color(variant.getColor())
                .size(variant.getSize())
                .price(effectivePrice)
                .stock(variant.getStock())
                .status(variant.getStatus())
                .build();
    }

    @Override
    public List<ProductVariantResponse> getVariantsByProductId(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return variantRepository.findByProductId(productId)
                .stream()
                .map(v -> toDto(v, product.getBasePrice()))
                .collect(Collectors.toList());
    }

    @Override
    public ProductVariantResponse getVariantById(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        return toDto(variant, variant.getProduct().getBasePrice());
    }

    @Override
    @Transactional
    public ProductVariantResponse createVariant(Long productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        LocalDateTime now = LocalDateTime.now();

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(request.getSku())
                .color(request.getColor())
                .size(request.getSize())
                .price(request.getPrice())
                .stock(request.getStock())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        variant = variantRepository.save(variant);
        return toDto(variant, product.getBasePrice());
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(Long variantId, ProductVariantRequest request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        variant.setSku(request.getSku());
        variant.setColor(request.getColor());
        variant.setSize(request.getSize());
        variant.setPrice(request.getPrice());
        variant.setStock(request.getStock());
        variant.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        variant.setUpdatedAt(LocalDateTime.now());

        variant = variantRepository.save(variant);
        return toDto(variant, variant.getProduct().getBasePrice());
    }

    @Override
    @Transactional
    public void deleteVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        // Soft delete
        variant.setStatus("INACTIVE");
        variant.setUpdatedAt(LocalDateTime.now());
        variantRepository.save(variant);
    }
}
