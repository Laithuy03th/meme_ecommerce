package com.example.MyWeb.service;

import com.example.MyWeb.dto.product.AdminProductRequest;
import com.example.MyWeb.dto.product.AdminProductResponse;
import org.springframework.data.domain.Page;

public interface AdminProductService {
    AdminProductResponse create(AdminProductRequest request);

    AdminProductResponse update(Long id, AdminProductRequest request);

    void delete(Long id);

    Page<AdminProductResponse> list(int page, int size);

    AdminProductResponse getDetail(Long id);
}
