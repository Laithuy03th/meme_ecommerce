package com.example.MyWeb.controller;

import com.example.MyWeb.dto.product.ProductDetailResponse;
import com.example.MyWeb.dto.product.ProductListItemResponse;
import com.example.MyWeb.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Ví dụ gọi:
     * GET /api/v1/products?page=0&size=20&sortBy=newest
     * GET /api/v1/products?category=dresses&page=0&size=20
     * GET /api/v1/products?keyword=hoodie&sortBy=priceAsc
     */
    @GetMapping
    public ResponseEntity<Page<ProductListItemResponse>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sortBy) {
        Page<ProductListItemResponse> result = productService.getProducts(keyword, category, page, size, sortBy);
        return ResponseEntity.ok(result);
    }

    /**
     * Chi tiết product theo slug
     * Ví dụ: GET /api/v1/products/slug/red-midi-dress
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductDetailResponse> getProductDetailBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getProductDetailBySlug(slug));
    }

    // Chi tiết product theo id (tiện cho admin / Postman)
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductDetailById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductDetailById(id));
    }
}
