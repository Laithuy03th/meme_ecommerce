package com.example.MyWeb.service;

import com.example.MyWeb.dto.product.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getActiveCategories();
}
