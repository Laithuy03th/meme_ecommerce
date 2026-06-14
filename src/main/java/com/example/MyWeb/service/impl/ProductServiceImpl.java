package com.example.MyWeb.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.example.MyWeb.repository.spec.ProductSpecifications;
import com.example.MyWeb.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

        private final ProductRepository productRepository;
        private final ObjectMapper objectMapper;

        private ProductListItemResponse toListItem(Product p) {
                String stockStatus = calculateStockStatus(p.getStockQuantity());

                return ProductListItemResponse.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .slug(p.getSlug())
                                .thumbnailUrl(p.getThumbnailUrl())
                                .price(p.getBasePrice())
                                .categorySlug(p.getCategory().getSlug())
                                .categoryName(p.getCategory().getName())
                                .shortDesc(p.getShortDesc())

                                .brand(p.getBrand())
                                .averageRating(p.getAverageRating())
                                .reviewCount(p.getReviewCount() != null ? p.getReviewCount() : 0)
                                .soldCount(p.getSoldCount() != null ? p.getSoldCount() : 0)
                                .stockStatus(stockStatus)
                                .isFeatured(p.getIsFeatured())

                                .createdAt(p.getCreatedAt())
                                .build();
        }

        @Override
        public Page<ProductListItemResponse> getProducts(
                        String keyword,
                        String categorySlug,
                        Double minPrice,
                        Double maxPrice,
                        String brand,
                        Double minRating,
                        int page,
                        int size,
                        String sortBy) {

                String kw = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim().toLowerCase();
                String catSlug = (categorySlug == null || categorySlug.trim().isEmpty()) ? null : categorySlug.trim();

                Sort sort = switch (sortBy) {
                        case "oldest" -> Sort.by("createdAt").ascending();
                        case "priceAsc" -> Sort.by("basePrice").ascending();
                        case "priceDesc" -> Sort.by("basePrice").descending();
                        case "bestSelling", "popular" -> Sort.by(Sort.Order.desc("soldCount"),
                                        Sort.Order.desc("createdAt"));
                        case "topRated" -> Sort.by(Sort.Order.desc("averageRating"),
                                        Sort.Order.desc("reviewCount"));
                        default -> Sort.by("createdAt").descending(); // newest
                };

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<Product> productPage = productRepository.findAll(
                                ProductSpecifications.search(kw, catSlug, minPrice, maxPrice, brand, minRating),
                                pageable);

                return productPage.map(this::toListItem);
        }

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

        private String calculateStockStatus(Integer stock) {
                if (stock == null || stock == 0) {
                        return "OUT_OF_STOCK";
                } else if (stock < 10) {
                        return "LOW_STOCK";
                } else {
                        return "IN_STOCK";
                }
        }

        @SuppressWarnings("unchecked")
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

        private ProductDetailResponse toDetail(Product p) {

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

                List<ProductVariantResponse> variantDtos = p.getVariants() == null ? List.of()
                                : p.getVariants()
                                                .stream()
                                                .filter(v -> v.getStatus() == null ||
                                                                "ACTIVE".equalsIgnoreCase(v.getStatus()))
                                                .map(v -> toVariantDto(v, p.getBasePrice()))
                                                .toList();

                String stockStatus = calculateStockStatus(p.getStockQuantity());

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

                                // Product metadata
                                .brand(p.getBrand())
                                .sku(p.getSku())
                                .weight(p.getWeight())

                                // Stock information
                                .stockQuantity(p.getStockQuantity())
                                .stockStatus(stockStatus)

                                .averageRating(p.getAverageRating())
                                .reviewCount(p.getReviewCount() != null ? p.getReviewCount() : 0)
                                .soldCount(p.getSoldCount() != null ? p.getSoldCount() : 0)
                                .viewCount(p.getViewCount() != null ? p.getViewCount() : 0)

                                // Media
                                .videoUrl(p.getVideoUrl())
                                .isFeatured(p.getIsFeatured())

                                .createdAt(p.getCreatedAt())
                                .updatedAt(p.getUpdatedAt())
                                .images(imageDtos)
                                .variants(variantDtos)

                                // Thông số kỹ thuật
                                .specifications(parseSpecifications(p.getSpecifications()))

                                .build();
        }

        @Override
        @Transactional
        public ProductDetailResponse getProductDetailBySlug(String slug) {
                Product p = productRepository.findBySlugAndStatus(slug, "ACTIVE")
                                .orElseThrow(() -> new RuntimeException("Product not found"));

                Integer currentViews = p.getViewCount() != null ? p.getViewCount() : 0;
                p.setViewCount(currentViews + 1);
                p.setUpdatedAt(LocalDateTime.now());
                productRepository.save(p);

                return toDetail(p);
        }

        @Override
        @Transactional
        public ProductDetailResponse getProductDetailById(Long id) {
                Product p = productRepository.findByIdAndStatus(id, "ACTIVE")
                                .orElseThrow(() -> new RuntimeException("Product not found"));

                Integer currentViews = p.getViewCount() != null ? p.getViewCount() : 0;
                p.setViewCount(currentViews + 1);
                p.setUpdatedAt(LocalDateTime.now());
                productRepository.save(p);

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

                // Suggestion ưu tiên theo soldCount — sản phẩm phổ biến nhất lên đầu
                Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("soldCount")));

                Page<Product> products = productRepository.findAll(
                                ProductSpecifications.search(kw, catSlug, null, null, null, null),
                                pageable);

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

                return Arrays.asList(
                                // Fashion (Váy, Áo)
                                new SearchKeywordSuggestion("Váy", "Fashion", "fashion"),
                                new SearchKeywordSuggestion("Áo", "Fashion", "fashion"),

                                // Home & Living (Bàn, Ghế)
                                new SearchKeywordSuggestion("Bàn", "Home & Living", "home-living"),
                                new SearchKeywordSuggestion("Ghế", "Home & Living", "home-living"),

                                // Beauty (Kem, Mặt nạ)
                                new SearchKeywordSuggestion("Kem", "Beauty", "beauty"),
                                new SearchKeywordSuggestion("Mặt nạ", "Beauty", "beauty"),

                                // Electronics (Điện thoại, Đồng hồ)
                                new SearchKeywordSuggestion("iPhone", "Electronics", "electronics"),
                                new SearchKeywordSuggestion("Samsung", "Electronics", "electronics"),
                                new SearchKeywordSuggestion("Đồng hồ", "Electronics", "electronics"));
        }
}
