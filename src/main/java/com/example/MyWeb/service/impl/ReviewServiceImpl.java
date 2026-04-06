package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.review.ReviewRequest;
import com.example.MyWeb.dto.review.ReviewResponse;
import com.example.MyWeb.exception.ResourceNotFoundException;
import com.example.MyWeb.model.CustomerProfile;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.model.Review;
import com.example.MyWeb.model.User;
import com.example.MyWeb.repository.CustomerProfileRepository;
import com.example.MyWeb.repository.OrderItemRepository;
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
    private final OrderItemRepository orderItemRepository;

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
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // VERIFY PURCHASE: User must have bought this product
        com.example.MyWeb.model.OrderItem orderItem = orderItemRepository.findReviewableOrderItem(
                request.getOrderItemId(), userId)
                .orElseThrow(() -> new RuntimeException(
                        "Order item not found or not delivered. You must purchase and receive this product to review."));

        // Verify order item belongs to the correct product
        if (!orderItem.getProduct().getId().equals(productId)) {
            throw new RuntimeException("Order item does not belong to this product");
        }

        // CHECK IF ALREADY REVIEWED THIS PURCHASE
        boolean alreadyReviewed = reviewRepository.existsByUser_IdAndOrderItem_Id(
                userId, orderItem.getId());

        if (alreadyReviewed) {
            throw new RuntimeException(
                    "You have already reviewed this purchase. You can only review once per purchase.");
        }

        // CREATE REVIEW LINKED TO PURCHASE
        Review review = Review.builder()
                .user(user)
                .product(product)
                .order(orderItem.getOrder())
                .orderItem(orderItem)
                .rating(request.getRating())
                .comment(request.getComment())
                .imageUrl(request.getImageUrl())
                .editCount(0)
                .isEdited(false)
                .build();

        review = reviewRepository.save(review);

        // Update product average rating and review count
        updateProductRating(productId);

        return toDto(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        updateProductRating(productId);
    }

    @Override
    @Transactional
    public void deleteReviewByUser(Long userId, Long reviewId) {
        Review review = reviewRepository.findByIdAndUser_Id(reviewId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        updateProductRating(productId);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request) {
        // Find review
        Review review = reviewRepository.findByIdAndUser_Id(reviewId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "Review not found or you don't have permission to edit it"));

        // CHECK EDIT LIMIT (Shopee: max 2 edits)
        if (review.getEditCount() >= 2) {
            throw new RuntimeException(
                    "You have reached the maximum edit limit (2 times). Cannot edit this review anymore.");
        }

        // Update review content
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        // Update image if provided
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            review.setImageUrl(request.getImageUrl());
        }

        // Track edit
        review.setEditCount(review.getEditCount() + 1);
        review.setIsEdited(true);

        reviewRepository.save(review);

        // Update product stats
        updateProductRating(review.getProduct().getId());

        return toDto(review);
    }

    private ReviewResponse toDto(Review review) {
        CustomerProfile profile = customerProfileRepository.findByUser(review.getUser()).orElse(null);
        String fullName = profile != null ? profile.getFullName() : review.getUser().getEmail();

        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .username(fullName)
                .userFullName(fullName)
                .productId(review.getProduct().getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrl(review.getImageUrl())

                // Edit tracking
                .editCount(review.getEditCount())
                .isEdited(review.getIsEdited())

                // Purchase verification
                .orderId(review.getOrder() != null ? review.getOrder().getId() : null)
                .orderNumber(review.getOrder() != null ? "ORD-" + review.getOrder().getId() : null)
                .verifiedPurchase(review.getOrderItem() != null)

                .adminReply(review.getAdminReply())
                .adminRepliedAt(review.getAdminRepliedAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse replyToReview(Long reviewId, String reply) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.setAdminReply(reply);
        review.setAdminRepliedAt(java.time.LocalDateTime.now());

        review = reviewRepository.save(review);
        return toDto(review);
    }

    private void updateProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // L10 FIX: Dùng SQL AVG chứ không load toàn bộ review vào RAM
        long reviewCount = reviewRepository.countByProduct_Id(productId);
        if (reviewCount == 0) {
            product.setAverageRating(null);
            product.setReviewCount(0);
        } else {
            Double avg = reviewRepository.getAverageRatingByProductId(productId);
            double roundedAverage = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
            product.setAverageRating(roundedAverage);
            product.setReviewCount((int) reviewCount);
        }
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReviewResponse> getAllReviews(int page, int size, Integer rating) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Review> reviews;
        if (rating != null) {
            reviews = reviewRepository.findByRatingOrderByCreatedAtDesc(rating, pageable);
        } else {
            reviews = reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return reviews.map(this::toDto);
    }

    @Override
    @Transactional
    public void toggleReviewVisibility(Long reviewId, boolean isVisible) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setIsVisible(isVisible);
        reviewRepository.save(review);
    }
}
