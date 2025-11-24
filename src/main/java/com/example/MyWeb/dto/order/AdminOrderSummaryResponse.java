// src/main/java/com/example/MyWeb/dto/order/AdminOrderSummaryResponse.java
package com.example.MyWeb.dto.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AdminOrderSummaryResponse {

    private Long id;

    private String userEmail;
    private String shippingFullName; // lấy từ order.address.fullName nếu có

    private Double totalAmount;
    private Double shippingFee;

    private String status; // PENDING / PAID / SHIPPED / COMPLETED / CANCELED
    private String paymentMethod; // COD / VNPAY / MOMO...
    private String paymentStatus; // UNPAID / PAID / FAILED

    private LocalDateTime createdAt;
}
