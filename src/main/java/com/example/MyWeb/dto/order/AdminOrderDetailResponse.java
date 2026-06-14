// src/main/java/com/example/MyWeb/dto/order/AdminOrderDetailResponse.java
package com.example.MyWeb.dto.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class AdminOrderDetailResponse {

    private Long id;

    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private Double totalAmount;
    private Double shippingFee;
    private String note;

    // User info
    private Long userId;
    private String userEmail;

    // Shipping info
    private String shippingFullName;
    private String shippingPhone;
    private String shippingAddressLine1;
    private String shippingWard;
    private String shippingDistrict;
    private String shippingProvince;
    private String shippingCountry;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<AdminOrderItemResponse> items;
}
