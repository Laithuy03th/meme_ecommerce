package com.example.MyWeb.controller;

import com.example.MyWeb.dto.review.AdminReplyRequest;
import com.example.MyWeb.dto.review.ReviewRequest;
import com.example.MyWeb.dto.review.ReviewResponse;
import com.example.MyWeb.security.CustomUserDetails;
import com.example.MyWeb.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/{productId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    @PostMapping("/{productId}/reviews")
    public ResponseEntity<ReviewResponse> addReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.addReview(userDetails.getId(), productId, request));
    }

    @DeleteMapping("/reviews/{reviewId}/me")
    public ResponseEntity<Void> deleteMyReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId) {
        reviewService.deleteReviewByUser(userDetails.getId(), reviewId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/products/reviews/{reviewId}/me
     * Update own review (max 2 edits allowed - Shopee rule)
     */
    @PutMapping("/reviews/{reviewId}/me")
    public ResponseEntity<ReviewResponse> updateMyReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(
                reviewService.updateReview(userDetails.getId(), reviewId, request));
    }

    // Admin endpoints
    @PostMapping("/reviews/{reviewId}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> replyToReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody AdminReplyRequest request) {
        return ResponseEntity.ok(reviewService.replyToReview(reviewId, request.getReply()));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
