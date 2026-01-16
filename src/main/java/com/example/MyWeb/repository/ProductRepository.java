package com.example.MyWeb.repository;

import com.example.MyWeb.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

        @Query(value = "SELECT p.* FROM products p " +
                        "JOIN categories c ON c.id = p.category_id " +
                        "WHERE p.status = 'ACTIVE' " +
                        "AND (CAST(:keyword AS VARCHAR) IS NULL OR LOWER(p.name) LIKE '%' || LOWER(CAST(:keyword AS VARCHAR)) || '%') "
                        +
                        "AND (CAST(:categorySlug AS VARCHAR) IS NULL OR c.slug = CAST(:categorySlug AS VARCHAR)) " +
                        "AND (:minPrice IS NULL OR p.base_price >= CAST(:minPrice AS DOUBLE PRECISION)) " +
                        "AND (:maxPrice IS NULL OR p.base_price <= CAST(:maxPrice AS DOUBLE PRECISION)) " +
                        "AND (CAST(:brand AS VARCHAR) IS NULL OR p.brand = CAST(:brand AS VARCHAR)) " +
                        "AND (:minRating IS NULL OR p.average_rating >= CAST(:minRating AS DOUBLE PRECISION))", countQuery = "SELECT COUNT(p.*) FROM products p "
                                        +
                                        "JOIN categories c ON c.id = p.category_id " +
                                        "WHERE p.status = 'ACTIVE' " +
                                        "AND (CAST(:keyword AS VARCHAR) IS NULL OR LOWER(p.name) LIKE '%' || LOWER(CAST(:keyword AS VARCHAR)) || '%') "
                                        +
                                        "AND (CAST(:categorySlug AS VARCHAR) IS NULL OR c.slug = CAST(:categorySlug AS VARCHAR)) "
                                        +
                                        "AND (:minPrice IS NULL OR p.base_price >= CAST(:minPrice AS DOUBLE PRECISION)) "
                                        +
                                        "AND (:maxPrice IS NULL OR p.base_price <= CAST(:maxPrice AS DOUBLE PRECISION)) "
                                        +
                                        "AND (CAST(:brand AS VARCHAR) IS NULL OR p.brand = CAST(:brand AS VARCHAR)) " +
                                        "AND (:minRating IS NULL OR p.average_rating >= CAST(:minRating AS DOUBLE PRECISION))", nativeQuery = true)
        Page<Product> searchProducts(
                        @Param("keyword") String keyword,
                        @Param("categorySlug") String categorySlug,
                        @Param("minPrice") Double minPrice,
                        @Param("maxPrice") Double maxPrice,
                        @Param("brand") String brand,
                        @Param("minRating") Double minRating,
                        Pageable pageable);

        boolean existsBySlug(String slug);

        Optional<Product> findBySlugAndStatus(String slug, String status);

        Optional<Product> findByIdAndStatus(Long id, String status);

        long countByStockQuantityLessThanEqual(Integer threshold);

        Page<Product> findByCategoryIdAndIdNotAndStatus(Long categoryId, Long id, String status, Pageable pageable);

        List<Product> findByNameContainingIgnoreCaseAndStatus(String name, String status);

        // For DataSeeder: count existing products in a category
        long countByCategory(com.example.MyWeb.model.Category category);
}
