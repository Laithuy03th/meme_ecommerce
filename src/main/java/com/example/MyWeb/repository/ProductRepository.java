package com.example.MyWeb.repository;

import com.example.MyWeb.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

        /**
         * Lấy product với Pessimistic Write Lock.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT p FROM Product p WHERE p.id = :id")
        Optional<Product> findByIdWithLock(@Param("id") Long id);

        // ====================================================================
        // ATOMIC STOCK OPERATIONS - Tầng DB chống race condition
        // ====================================================================

        /**
         * Trừ tồn kho nguyên tử (Atomic Decrease).
         * DB tự khóa dòng và thực hiện trừ trong 1 bước DUY NHẤT.
         * Điều kiện WHERE stock_quantity >= qty đảm bảo không bao giờ bán âm.
         *
         * @return số dòng bị ảnh hưởng: 1 = thành công, 0 = hết hàng
         */
        @Modifying
        @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :qty, "
                        + "p.soldCount = p.soldCount + :qty, "
                        + "p.updatedAt = CURRENT_TIMESTAMP "
                        + "WHERE p.id = :productId AND p.stockQuantity >= :qty")
        int atomicDecreaseStock(@Param("productId") Long productId, @Param("qty") int qty);

        /**
         * Tăng tồn kho nguyên tử (Atomic Increase) - dùng khi hủy đơn hàng.
         */
        @Modifying
        @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :qty, "
                        + "p.soldCount = CASE WHEN p.soldCount >= :qty THEN p.soldCount - :qty ELSE 0 END, "
                        + "p.updatedAt = CURRENT_TIMESTAMP "
                        + "WHERE p.id = :productId")
        int atomicIncreaseStock(@Param("productId") Long productId, @Param("qty") int qty);

        @Query(value = "SELECT p.* FROM products p " +
                        "JOIN categories c ON c.id = p.category_id " +
                        "WHERE p.status = 'ACTIVE' " +
                        "AND (CAST(:keyword AS VARCHAR) IS NULL OR " +
                        "(LOWER(p.name) LIKE '%' || LOWER(CAST(:keyword AS VARCHAR)) || '%' OR " +
                        // Cho description: Tìm từ nguyên vẹn (có khoảng trắng bao quanh hoặc đầu/cuối
                        // câu)
                        "LOWER(p.short_desc) LIKE '% ' || LOWER(CAST(:keyword AS VARCHAR)) || ' %' OR " +
                        "LOWER(p.short_desc) LIKE LOWER(CAST(:keyword AS VARCHAR)) || ' %' OR " +
                        "LOWER(p.short_desc) LIKE '% ' || LOWER(CAST(:keyword AS VARCHAR)) OR " +
                        // Long desc tương tự
                        "LOWER(p.long_desc) LIKE '% ' || LOWER(CAST(:keyword AS VARCHAR)) || ' %' OR " +
                        "LOWER(p.long_desc) LIKE LOWER(CAST(:keyword AS VARCHAR)) || ' %' OR " +
                        "LOWER(p.long_desc) LIKE '% ' || LOWER(CAST(:keyword AS VARCHAR)))) "
                        +
                        "AND (CAST(:categorySlug AS VARCHAR) IS NULL OR c.slug = CAST(:categorySlug AS VARCHAR)) " +
                        "AND (:minPrice IS NULL OR p.base_price >= CAST(:minPrice AS DOUBLE PRECISION)) " +
                        "AND (:maxPrice IS NULL OR p.base_price <= CAST(:maxPrice AS DOUBLE PRECISION)) " +
                        "AND (CAST(:brand AS VARCHAR) IS NULL OR p.brand = CAST(:brand AS VARCHAR)) " +
                        "AND (:minRating IS NULL OR p.average_rating >= CAST(:minRating AS DOUBLE PRECISION))", countQuery = "SELECT COUNT(p.*) FROM products p "
                                        +
                                        "JOIN categories c ON c.id = p.category_id " +
                                        "WHERE p.status = 'ACTIVE' " +
                                        "AND (CAST(:keyword AS VARCHAR) IS NULL OR " +
                                        "(LOWER(p.name) LIKE '%' || LOWER(CAST(:keyword AS VARCHAR)) || '%' OR " +
                                        "LOWER(p.short_desc) LIKE '% ' || LOWER(CAST(:keyword AS VARCHAR)) || ' %' OR "
                                        +
                                        "LOWER(p.short_desc) LIKE LOWER(CAST(:keyword AS VARCHAR)) || ' %' OR " +
                                        "LOWER(p.short_desc) LIKE '% ' || LOWER(CAST(:keyword AS VARCHAR)) OR " +
                                        "LOWER(p.long_desc) LIKE '% ' || LOWER(CAST(:keyword AS VARCHAR)) || ' %' OR " +
                                        "LOWER(p.long_desc) LIKE LOWER(CAST(:keyword AS VARCHAR)) || ' %' OR " +
                                        "LOWER(p.long_desc) LIKE '% ' || LOWER(CAST(:keyword AS VARCHAR)))) "
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
