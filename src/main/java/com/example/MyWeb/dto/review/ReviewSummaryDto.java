package com.example.MyWeb.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryDto {
    private Long productId;
    private double averageRating;
    private int totalReviews;
    private Map<Integer, Long> starCounts; // e.g. {5: 10, 4: 2}
}
