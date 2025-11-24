package com.example.MyWeb.repository;

import com.example.MyWeb.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

        Page<Product> findByStatusAndNameContainingIgnoreCase(
                        String status,
                        String keyword,
                        Pageable pageable);

        Page<Product> findByStatusAndCategory_SlugAndNameContainingIgnoreCase(
                        String status,
                        String categorySlug,
                        String keyword,
                        Pageable pageable);

        boolean existsBySlug(String slug);

        Optional<Product> findBySlugAndStatus(String slug, String status);

        Optional<Product> findByIdAndStatus(Long id, String status);
}
