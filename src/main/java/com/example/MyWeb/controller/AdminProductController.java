// src/main/java/com/example/MyWeb/controller/AdminProductController.java
package com.example.MyWeb.controller;

import com.example.MyWeb.dto.product.AdminProductRequest;
import com.example.MyWeb.dto.product.AdminProductResponse;
import com.example.MyWeb.service.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @PostMapping
    public ResponseEntity<AdminProductResponse> create(@Valid @RequestBody AdminProductRequest request) {
        return ResponseEntity.ok(adminProductService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminProductRequest request) {
        return ResponseEntity.ok(adminProductService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminProductService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<AdminProductResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminProductService.list(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminProductResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(adminProductService.getDetail(id));
    }
}
