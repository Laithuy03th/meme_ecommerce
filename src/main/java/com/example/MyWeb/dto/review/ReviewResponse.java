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
    private String userFullName;
    private Integer rating;
    private String comment;
    private String imageUrl;
    private String adminReply;
    private LocalDateTime adminRepliedAt;
    private LocalDateTime createdAt;
}
