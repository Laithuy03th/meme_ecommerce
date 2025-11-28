package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.category.CategoryRequest;
import com.example.MyWeb.dto.category.CategoryResponse;
import com.example.MyWeb.model.Category;
import com.example.MyWeb.repository.CategoryRepository;
import com.example.MyWeb.service.AdminCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private final CategoryRepository categoryRepository;

    private CategoryResponse toDto(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .build();
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return toDto(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        // Auto-generate slug if empty
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = request.getName().toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-");
        }

        // Check duplicate slug
        if (categoryRepository.existsBySlug(slug)) {
            throw new RuntimeException("Category slug already exists: " + slug);
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .parent(parent)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        category = categoryRepository.save(category);
        return toDto(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        // Update slug if provided
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            if (!request.getSlug().equals(category.getSlug())) {
                if (categoryRepository.existsBySlug(request.getSlug())) {
                    throw new RuntimeException("Category slug already exists: " + request.getSlug());
                }
                category.setSlug(request.getSlug());
            }
        }

        // Update parent
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        category = categoryRepository.save(category);
        return toDto(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Soft delete by setting status to INACTIVE
        category.setStatus("INACTIVE");
        categoryRepository.save(category);
    }
}
