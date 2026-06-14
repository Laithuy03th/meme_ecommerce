// src/main/java/com/example/MyWeb/service/impl/AdminProductServiceImpl.java
package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.product.AdminProductRequest;
import com.example.MyWeb.dto.product.AdminProductResponse;
import com.example.MyWeb.model.Category;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.model.ProductImage;
import com.example.MyWeb.model.ProductVariant;
import com.example.MyWeb.repository.CategoryRepository;
import com.example.MyWeb.repository.ProductRepository;
import com.example.MyWeb.repository.ProductVariantRepository;
import com.example.MyWeb.service.AdminProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ObjectMapper objectMapper;

    private AdminProductResponse toDto(Product p) {
        return AdminProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .shortDesc(p.getShortDesc())
                .longDesc(p.getLongDesc())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .categorySlug(p.getCategory() != null ? p.getCategory().getSlug() : null)
                .basePrice(p.getBasePrice())
                .stockQuantity(p.getStockQuantity())
                .thumbnailUrl(p.getThumbnailUrl())
                .imageUrls(p.getImages() != null ? p.getImages().stream().map(ProductImage::getImageUrl)
                        .collect(java.util.stream.Collectors.toList()) : null)

                .brand(p.getBrand())
                .sku(p.getSku())
                .weight(p.getWeight())
                .isFeatured(p.getIsFeatured())
                .videoUrl(p.getVideoUrl())
                .averageRating(p.getAverageRating())
                .reviewCount(p.getReviewCount())
                .soldCount(p.getSoldCount())
                .viewCount(p.getViewCount())

                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())

                // Thông số kỹ thuật
                .specifications(parseSpecifications(p.getSpecifications()))

                .build();
    }

    private Map<String, String> parseSpecifications(String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {
            });
        } catch (Exception e) {
            log.warn("Cannot parse specifications JSON: {}", e.getMessage());
            return null;
        }
    }

    private String specsToJson(Map<String, String> specs) {
        if (specs == null || specs.isEmpty())
            return null;
        try {
            return objectMapper.writeValueAsString(specs);
        } catch (Exception e) {
            log.warn("Cannot serialize specifications: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public AdminProductResponse create(AdminProductRequest request) {

        String slug = request.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlug(request.getName());
        }

        if (productRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Slug đã tồn tại: " + slug + ". Vui lòng chọn slug khác.");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        LocalDateTime now = LocalDateTime.now();

        Product p = Product.builder()
                .name(request.getName())
                .slug(slug)
                .shortDesc(request.getShortDesc())
                .longDesc(request.getLongDesc())
                .category(category)
                .basePrice(request.getBasePrice())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .thumbnailUrl(request.getThumbnailUrl())

                .brand(request.getBrand())
                .sku(request.getSku())
                .weight(request.getWeight())
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .videoUrl(request.getVideoUrl())

                // Specifications
                .specifications(specsToJson(request.getSpecifications()))

                .averageRating(0.0)
                .reviewCount(0)
                .soldCount(0)
                .viewCount(0)

                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .images(new ArrayList<>())
                .build();

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (String url : request.getImageUrls()) {
                ProductImage pi = ProductImage.builder()
                        .product(p)
                        .imageUrl(url)
                        .thumbnail(false)
                        .sortOrder(0)
                        .build();
                p.getImages().add(pi);
            }
        }

        p = productRepository.save(p);

        ProductVariant defaultVariant = ProductVariant.builder()
                .product(p)
                .sku(p.getSku() != null ? p.getSku() + "-DEF" : p.getSlug().toUpperCase() + "-DEF")
                .price(p.getBasePrice())
                .stock(p.getStockQuantity())
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        productVariantRepository.save(defaultVariant);

        return toDto(p);
    }

    private String generateSlug(String name) {
        if (name == null)
            return "";
        String slug = name.toLowerCase()
                .replace("đ", "d")
                .replace("Đ", "d");
        slug = java.text.Normalizer.normalize(slug, java.text.Normalizer.Form.NFD);
        slug = slug.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        slug = slug.replaceAll("[^a-z0-9\\s-]", "");
        slug = slug.trim().replaceAll("\\s+", "-");
        return slug;
    }

    @Override
    public AdminProductResponse update(Long id, AdminProductRequest request) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (request.getSlug() != null && !request.getSlug().equals(p.getSlug())) {
            if (productRepository.existsBySlug(request.getSlug())) {
                throw new IllegalArgumentException(
                        "Slug đã tồn tại: " + request.getSlug() + ". Vui lòng chọn slug khác.");
            }
            p.setSlug(request.getSlug());
        }

        p.setName(request.getName());
        p.setShortDesc(request.getShortDesc());
        p.setLongDesc(request.getLongDesc());
        p.setBasePrice(request.getBasePrice());
        p.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        p.setThumbnailUrl(request.getThumbnailUrl());
        p.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        p.setBrand(request.getBrand());
        p.setSku(request.getSku());
        p.setWeight(request.getWeight());
        p.setIsFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false);
        p.setVideoUrl(request.getVideoUrl());
        p.setSpecifications(specsToJson(request.getSpecifications()));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            p.setCategory(category);
        }

        if (request.getImageUrls() != null) {
            if (p.getImages() == null) {
                p.setImages(new ArrayList<>());
            } else {
                p.getImages().clear();
            }
            for (String url : request.getImageUrls()) {
                ProductImage pi = ProductImage.builder()
                        .product(p)
                        .imageUrl(url)
                        .thumbnail(false)
                        .sortOrder(0)
                        .build();
                p.getImages().add(pi);
            }
        }

        p.setUpdatedAt(LocalDateTime.now());

        return toDto(productRepository.save(p));
    }

    @Override
    public void delete(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        p.setStatus("INACTIVE");
        p.setUpdatedAt(LocalDateTime.now());
        productRepository.save(p);
    }

    @Override
    public AdminProductResponse getDetail(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return toDto(p);
    }

    @Override
    public Page<AdminProductResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findAll(pageable);
        return productPage.map(this::toDto);
    }
}
