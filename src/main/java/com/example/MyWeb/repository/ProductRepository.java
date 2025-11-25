package com.example.MyWeb.repository;

import com.example.MyWeb.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

        @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' " +
                        "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:categorySlug IS NULL OR p.category.slug = :categorySlug) " +
                        "AND (:minPrice IS NULL OR p.basePrice >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)")
        Page<Product> searchProducts(
                        @Param("keyword") String keyword,
                        @Param("categorySlug") String categorySlug,
                        @Param("minPrice") Double minPrice,
                        @Param("maxPrice") Double maxPrice,
                        Pageable pageable);

        boolean existsBySlug(String slug);

        Optional<Product> findBySlugAndStatus(String slug, String status);

        Optional<Product> findByIdAndStatus(Long id, String status);
}
