package com.example.MyWeb.dto.review;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long id;
    private Long userId;
    private String username;
    private String userFullName; // Keep for backward compatibility
    private Long productId;
    private Integer rating;
    private String comment;
    private String imageUrl;

    // Edit tracking (Shopee-level feature)
    private Integer editCount;
    private Boolean isEdited;

    // Purchase verification
    private Long orderId;
    private String orderNumber;
    private Boolean verifiedPurchase; // "Verified Purchase" badge

    private String adminReply;
    private LocalDateTime adminRepliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
