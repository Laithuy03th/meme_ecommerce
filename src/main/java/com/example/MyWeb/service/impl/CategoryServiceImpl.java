package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.product.CategoryResponse;
import com.example.MyWeb.model.Category;
import com.example.MyWeb.repository.CategoryRepository;
import com.example.MyWeb.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private CategoryResponse toDto(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .build();
    }

    @Override
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findAllByStatusOrderBySortOrderAsc("ACTIVE")
                .stream()
                .map(this::toDto)
                .toList();
    }
}
