package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.review.ReviewRequest;
import com.example.MyWeb.dto.review.ReviewResponse;
import com.example.MyWeb.exception.ResourceNotFoundException;
import com.example.MyWeb.model.CustomerProfile;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.model.Review;
import com.example.MyWeb.model.User;
import com.example.MyWeb.repository.CustomerProfileRepository;
import com.example.MyWeb.repository.ProductRepository;
import com.example.MyWeb.repository.ReviewRepository;
import com.example.MyWeb.repository.UserRepository;
import com.example.MyWeb.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CustomerProfileRepository customerProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getProductReviews(Long productId) {
        List<Review> reviews = reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId);
        return reviews.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ReviewResponse addReview(Long userId, Long productId, ReviewRequest request) {
        // Optional: Check if user already reviewed this product
        if (reviewRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            throw new RuntimeException("You have already reviewed this product");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .imageUrl(request.getImageUrl())
                .build();

        review = reviewRepository.save(review);
        return toDto(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    @Override
    @Transactional
    public void deleteReviewByUser(Long userId, Long reviewId) {
        Review review = reviewRepository.findByIdAndUser_Id(reviewId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        reviewRepository.delete(review);
    }

    private ReviewResponse toDto(Review review) {
        CustomerProfile profile = customerProfileRepository.findByUser(review.getUser()).orElse(null);
        String fullName = profile != null ? profile.getFullName() : review.getUser().getEmail();

        return ReviewResponse.builder()
                .id(review.getId())
                .userFullName(fullName)
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
