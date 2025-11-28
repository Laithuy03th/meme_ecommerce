package com.example.MyWeb.service;

import com.example.MyWeb.dto.category.CategoryRequest;
import com.example.MyWeb.dto.category.CategoryResponse;
import java.util.List;

public interface AdminCategoryService {
    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}
