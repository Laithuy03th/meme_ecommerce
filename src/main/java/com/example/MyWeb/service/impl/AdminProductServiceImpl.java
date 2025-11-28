// src/main/java/com/example/MyWeb/service/impl/AdminProductServiceImpl.java
package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.product.AdminProductRequest;
import com.example.MyWeb.dto.product.AdminProductResponse;
import com.example.MyWeb.model.Category;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.repository.CategoryRepository;
import com.example.MyWeb.repository.ProductRepository;
import com.example.MyWeb.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

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
                .status(p.getStatus())
                .build();
    }

    @Override
    public AdminProductResponse create(AdminProductRequest request) {

        String slug = request.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlug(request.getName());
        }

        if (productRepository.existsBySlug(slug)) {
            // Nếu auto-generated slug trùng, thêm suffix random hoặc timestamp (đơn giản
            // hoá: throw error để admin tự sửa)
            // Hoặc tốt hơn: append random string
            slug = slug + "-" + System.currentTimeMillis();
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
                .stockQuantity(request.getStockQuantity())
                .thumbnailUrl(request.getThumbnailUrl())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        p = productRepository.save(p);

        return toDto(p);
    }

    private String generateSlug(String name) {
        if (name == null)
            return "";
        String slug = name.toLowerCase();
        slug = java.text.Normalizer.normalize(slug, java.text.Normalizer.Form.NFD);
        slug = slug.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        slug = slug.replaceAll("[^a-z0-9\\s-]", "");
        slug = slug.replaceAll("\\s+", "-");
        return slug;
    }

    @Override
    public AdminProductResponse update(Long id, AdminProductRequest request) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Nếu đổi slug, kiểm tra trùng
        if (request.getSlug() != null && !request.getSlug().equals(p.getSlug())) {
            if (productRepository.existsBySlug(request.getSlug())) {
                throw new RuntimeException("Product slug already exists");
            }
            p.setSlug(request.getSlug());
        }

        if (request.getName() != null)
            p.setName(request.getName());
        if (request.getShortDesc() != null)
            p.setShortDesc(request.getShortDesc());
        if (request.getLongDesc() != null)
            p.setLongDesc(request.getLongDesc());
        if (request.getBasePrice() != null)
            p.setBasePrice(request.getBasePrice());
        if (request.getStockQuantity() != null)
            p.setStockQuantity(request.getStockQuantity());
        if (request.getThumbnailUrl() != null)
            p.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getStatus() != null)
            p.setStatus(request.getStatus());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            p.setCategory(category);
        }

        p.setUpdatedAt(LocalDateTime.now());

        return toDto(productRepository.save(p));
    }

    @Override
    public void delete(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Option 1: Xoá cứng
        // productRepository.delete(p);

        // Option 2: chuẩn thực tế hơn – chuyển sang INACTIVE:
        p.setStatus("INACTIVE");
        p.setUpdatedAt(LocalDateTime.now());
        productRepository.save(p);
    }

    @Override
    public Page<AdminProductResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findAll(pageable);
        return productPage.map(this::toDto);
    }

    @Override
    public AdminProductResponse getDetail(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return toDto(p);
    }
}
