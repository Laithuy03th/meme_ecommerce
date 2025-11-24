package com.example.MyWeb.dto.admin;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderSummaryResponse {
    private Long id;
    private String status; // PENDING / PAID / ...
    private String paymentStatus; // UNPAID / PAID / FAILED
    private Double totalAmount;
    private Double shippingFee;
    private LocalDateTime createdAt;
}
