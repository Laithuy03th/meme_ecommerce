package com.example.MyWeb.service.impl;

import java.util.Comparator;
import java.util.Arrays;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.example.MyWeb.dto.product.ProductDetailResponse;
import com.example.MyWeb.dto.product.ProductImageResponse;
import com.example.MyWeb.dto.product.ProductListItemResponse;
import com.example.MyWeb.dto.product.ProductSuggestionResponse;
import com.example.MyWeb.dto.product.ProductVariantResponse;
import com.example.MyWeb.dto.product.SearchKeywordSuggestion;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.model.ProductImage;
import com.example.MyWeb.model.ProductVariant;
import com.example.MyWeb.repository.ProductRepository;
import com.example.MyWeb.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

        private final ProductRepository productRepository;

        // ================== LIST ==================

        private ProductListItemResponse toListItem(Product p) {
                return ProductListItemResponse.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .slug(p.getSlug())
                                .thumbnailUrl(p.getThumbnailUrl())
                                .price(p.getBasePrice())
                                .categorySlug(p.getCategory().getSlug())
                                .categoryName(p.getCategory().getName())
                                .createdAt(p.getCreatedAt())
                                .build();
        }

        @Override
        public Page<ProductListItemResponse> getProducts(
                        String keyword,
                        String categorySlug,
                        Double minPrice,
                        Double maxPrice,
                        int page,
                        int size,
                        String sortBy) {

                String kw = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim().toLowerCase();
                String catSlug = (categorySlug == null || categorySlug.trim().isEmpty()) ? null : categorySlug.trim();

                Sort sort;
                switch (sortBy) {
                        case "oldest" -> sort = Sort.by("created_at").ascending();
                        case "priceAsc" -> sort = Sort.by("base_price").ascending();
                        case "priceDesc" -> sort = Sort.by("base_price").descending();
                        default -> sort = Sort.by("created_at").descending(); // newest
                }

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<Product> productPage = productRepository.searchProducts(kw, catSlug, minPrice, maxPrice, pageable);

                return productPage.map(this::toListItem);
        }

        // ================== DETAIL ==================

        private ProductImageResponse toImageDto(ProductImage img) {
                return ProductImageResponse.builder()
                                .id(img.getId())
                                .imageUrl(img.getImageUrl())
                                .thumbnail(img.isThumbnail())
                                .sortOrder(img.getSortOrder())
                                .build();
        }

        private ProductVariantResponse toVariantDto(ProductVariant v, Double basePrice) {
                Double effectivePrice = (v.getPrice() != null) ? v.getPrice() : basePrice;

                return ProductVariantResponse.builder()
                                .id(v.getId())
                                .sku(v.getSku())
                                .color(v.getColor())
                                .size(v.getSize())
                                .price(effectivePrice)
                                .stock(v.getStock())
                                .status(v.getStatus())
                                .build();
        }

        private ProductDetailResponse toDetail(Product p) {
                // Sort images theo sortOrder rồi id
                List<ProductImageResponse> imageDtos = p.getImages() == null ? List.of()
                                : p.getImages()
                                                .stream()
                                                .sorted(Comparator
                                                                .comparing((ProductImage img) -> img
                                                                                .getSortOrder() == null ? 0
                                                                                                : img.getSortOrder())
                                                                .thenComparing(ProductImage::getId))
                                                .map(this::toImageDto)
                                                .toList();

                // Lọc variants ACTIVE (nếu chưa dùng thì list rỗng)
                List<ProductVariantResponse> variantDtos = p.getVariants() == null ? List.of()
                                : p.getVariants()
                                                .stream()
                                                .filter(v -> v.getStatus() == null ||
                                                                "ACTIVE".equalsIgnoreCase(v.getStatus()))
                                                .map(v -> toVariantDto(v, p.getBasePrice()))
                                                .toList();

                return ProductDetailResponse.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .slug(p.getSlug())
                                .shortDesc(p.getShortDesc())
                                .longDesc(p.getLongDesc())
                                .categorySlug(p.getCategory().getSlug())
                                .categoryName(p.getCategory().getName())
                                .basePrice(p.getBasePrice())
                                .thumbnailUrl(p.getThumbnailUrl())
                                .status(p.getStatus())
                                .createdAt(p.getCreatedAt())
                                .updatedAt(p.getUpdatedAt())
                                .images(imageDtos)
                                .variants(variantDtos)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public ProductDetailResponse getProductDetailBySlug(String slug) {
                Product p = productRepository.findBySlugAndStatus(slug, "ACTIVE")
                                .orElseThrow(() -> new RuntimeException("Product not found"));
                return toDetail(p);
        }

        @Override
        @Transactional(readOnly = true)
        public ProductDetailResponse getProductDetailById(Long id) {
                Product p = productRepository.findByIdAndStatus(id, "ACTIVE")
                                .orElseThrow(() -> new RuntimeException("Product not found"));
                return toDetail(p);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<ProductListItemResponse> getRelatedProducts(Long productId, int page, int size) {
                Product product = productRepository.findByIdAndStatus(productId, "ACTIVE")
                                .orElseThrow(() -> new RuntimeException("Product not found"));

                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

                Page<Product> relatedProducts = productRepository.findByCategoryIdAndIdNotAndStatus(
                                product.getCategory().getId(),
                                productId,
                                "ACTIVE",
                                pageable);

                return relatedProducts.map(this::toListItem);
        }

        @Override
        @Transactional(readOnly = true)
        public List<ProductSuggestionResponse> getSearchSuggestions(String keyword, String categorySlug, int limit) {
                String kw = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim().toLowerCase();
                String catSlug = (categorySlug == null || categorySlug.trim().isEmpty()) ? null : categorySlug.trim();

                Pageable pageable = PageRequest.of(0, limit);
                Page<Product> products = productRepository.searchProducts(kw, catSlug, null, null, pageable);

                return products.getContent().stream()
                                .map(p -> ProductSuggestionResponse.builder()
                                                .id(p.getId())
                                                .name(p.getName())
                                                .slug(p.getSlug())
                                                .thumbnailUrl(p.getThumbnailUrl())
                                                .price(p.getBasePrice())
                                                .categoryName(p.getCategory().getName())
                                                .build())
                                .toList();
        }

        @Override
        public List<SearchKeywordSuggestion> getPopularSearchKeywords() {
                // Return curated list of popular search keywords organized by category
                return Arrays.asList(
                                // Fashion
                                new SearchKeywordSuggestion("dress", "Fashion", "fashion"),
                                new SearchKeywordSuggestion("shoes", "Fashion", "fashion"),
                                new SearchKeywordSuggestion("jacket", "Fashion", "fashion"),
                                new SearchKeywordSuggestion("sneakers", "Fashion", "fashion"),
                                new SearchKeywordSuggestion("boots", "Fashion", "fashion"),
                                new SearchKeywordSuggestion("t-shirt", "Fashion", "fashion"),

                                // Electronics
                                new SearchKeywordSuggestion("headphones", "Electronics", "electronics"),
                                new SearchKeywordSuggestion("watch", "Electronics", "electronics"),
                                new SearchKeywordSuggestion("wireless", "Electronics", "electronics"),

                                // Beauty
                                new SearchKeywordSuggestion("serum", "Beauty", "beauty"),
                                new SearchKeywordSuggestion("cream", "Beauty", "beauty"),
                                new SearchKeywordSuggestion("face care", "Beauty", "beauty"),

                                // Home & Living
                                new SearchKeywordSuggestion("lamp", "Home & Living", "home-living"),
                                new SearchKeywordSuggestion("chair", "Home & Living", "home-living"),
                                new SearchKeywordSuggestion("desk", "Home & Living", "home-living"));
        }
}
