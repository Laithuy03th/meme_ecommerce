package com.example.MyWeb.repository;

import com.example.MyWeb.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProduct_IdOrderByCreatedAtDesc(Long productId);

    Optional<Review> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    boolean existsByUser_IdAndOrderItem_Id(Long userId, Long orderItemId);

    Optional<Review> findByUser_IdAndOrderItem_Id(Long userId, Long orderItemId);

    List<Review> findByUser_IdAndProduct_IdOrderByCreatedAtDesc(Long userId, Long productId);

    org.springframework.data.domain.Page<Review> findAllByOrderByCreatedAtDesc(
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Review> findByRatingOrderByCreatedAtDesc(Integer rating,
            org.springframework.data.domain.Pageable pageable);

    long countByProduct_Id(Long productId);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRatingByProductId(@org.springframework.data.repository.query.Param("productId") Long productId);
}
