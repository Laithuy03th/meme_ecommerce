package com.example.MyWeb.controller.admin;

import com.example.MyWeb.dto.review.ReviewResponse;
import com.example.MyWeb.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    // Get All Reviews (Dashboard)
    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer rating) {
        return ResponseEntity.ok(reviewService.getAllReviews(page, size, rating));
    }

    // Toggle Visibility (Hide/Show)
    @PutMapping("/{reviewId}/visibility")
    public ResponseEntity<Void> toggleVisibility(
            @PathVariable Long reviewId,
            @RequestParam boolean isVisible) {
        reviewService.toggleReviewVisibility(reviewId, isVisible);
        return ResponseEntity.ok().build();
    }
}
