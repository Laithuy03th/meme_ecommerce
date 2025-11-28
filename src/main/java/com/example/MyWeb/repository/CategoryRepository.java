package com.example.MyWeb.repository;

import com.example.MyWeb.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<Category> findAllByStatusOrderBySortOrderAsc(String status);

    boolean existsBySlug(String slug);
}
