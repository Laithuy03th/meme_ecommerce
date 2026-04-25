package com.example.MyWeb.dto.order;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailResponse {
    private Long id;
    private String orderNumber;
    private LocalDateTime createdAt;
    private String status;
    private List<TimelineStep> timeline;
    private List<OrderItemDto> items;
    private Double subtotal;
    private Double shippingFee;
    private Double totalAmount;
    private ShippingAddressDto shippingAddress;
    private PaymentMethodDto paymentMethod;
    private LocalDateTime deliveredAt;


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimelineStep {
        private String status;
        private LocalDateTime timestamp;
        private boolean completed;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemDto {
        private Long id;
        private Long productId;
        private String productName;
        private String productImageUrl;
        private String variantInfo;
        private int quantity;
        private Double price;
        private boolean hasReviewed;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ShippingAddressDto {
        private String fullName;
        private String addressLine;
        private String phone;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentMethodDto {
        private String type;
        private String last4;
    }
}
