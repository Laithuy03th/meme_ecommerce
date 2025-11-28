package com.example.MyWeb.controller;

import com.example.MyWeb.dto.product.ProductVariantRequest;
import com.example.MyWeb.dto.product.ProductVariantResponse;
import com.example.MyWeb.service.AdminProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/products/{productId}/variants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductVariantController {

    private final AdminProductVariantService variantService;

    @GetMapping
    public ResponseEntity<List<ProductVariantResponse>> getVariantsByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(variantService.getVariantsByProductId(productId));
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<ProductVariantResponse> getVariantById(@PathVariable Long variantId) {
        return ResponseEntity.ok(variantService.getVariantById(variantId));
    }

    @PostMapping
    public ResponseEntity<ProductVariantResponse> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(variantService.createVariant(productId, request));
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<ProductVariantResponse> updateVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(variantService.updateVariant(variantId, request));
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long variantId) {
        variantService.deleteVariant(variantId);
        return ResponseEntity.noContent().build();
    }
}
