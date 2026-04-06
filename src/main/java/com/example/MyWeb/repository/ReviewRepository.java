package com.example.MyWeb.repository;

import com.example.MyWeb.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProduct_IdOrderByCreatedAtDesc(Long productId);

    Optional<Review> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    // NEW: Purchase-based review queries (Shopee-level)
    /**
     * Check if user already reviewed this specific purchase
     */
    boolean existsByUser_IdAndOrderItem_Id(Long userId, Long orderItemId);

    /**
     * Find review by user and order item (for edit)
     */
    Optional<Review> findByUser_IdAndOrderItem_Id(Long userId, Long orderItemId);

    /**
     * Get all reviews by user for a product (across multiple purchases)
     */
    List<Review> findByUser_IdAndProduct_IdOrderByCreatedAtDesc(Long userId, Long productId);

    // Admin: Get all reviews with pagination
    org.springframework.data.domain.Page<Review> findAllByOrderByCreatedAtDesc(
            org.springframework.data.domain.Pageable pageable);

    // Admin: Filter by rating
    org.springframework.data.domain.Page<Review> findByRatingOrderByCreatedAtDesc(Integer rating,
            org.springframework.data.domain.Pageable pageable);

    // L10 FIX: Đếm và tính trung bình bằng SQL — không load dữ liệu vào RAM
    long countByProduct_Id(Long productId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRatingByProductId(@org.springframework.data.repository.query.Param("productId") Long productId);
}
