package com.example.MyWeb.dto.order;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderListResponse {
    private Long id;
    private String orderNumber;
    private LocalDateTime createdAt;
    private String status;
    private int itemCount;
    private Double totalAmount;
    private String firstItemImageUrl;
}
