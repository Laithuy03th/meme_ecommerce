package com.example.MyWeb.dto.order;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderListResponse {
    private Long id;
    private String orderNumber; // "ORD-001"
    private LocalDateTime createdAt;
    private String status; // DELIVERED, PROCESSING...
    private int itemCount;
    private Double totalAmount;
    private String firstItemImageUrl; // For the thumbnail
}
