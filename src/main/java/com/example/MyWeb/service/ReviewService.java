package com.example.MyWeb.service;

import com.example.MyWeb.dto.review.ReviewRequest;
import com.example.MyWeb.dto.review.ReviewResponse;

import java.util.List;

public interface ReviewService {

    List<ReviewResponse> getProductReviews(Long productId);

    ReviewResponse addReview(Long userId, Long productId, ReviewRequest request);

    void deleteReview(Long reviewId);

    void deleteReviewByUser(Long userId, Long reviewId);

    ReviewResponse replyToReview(Long reviewId, String reply);

    ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request);

    // Admin methods
    org.springframework.data.domain.Page<ReviewResponse> getAllReviews(int page, int size, Integer rating);

    void toggleReviewVisibility(Long reviewId, boolean isVisible);
}
