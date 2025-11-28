package com.example.MyWeb.dto.order;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimelineStep {
        private String status;
        private LocalDateTime timestamp;
        private boolean completed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemDto {
        private Long id;
        private String productName;
        private String productImageUrl;
        private String variantInfo; // "Color: Black"
        private int quantity;
        private Double price;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShippingAddressDto {
        private String fullName;
        private String addressLine;
        private String phone;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentMethodDto {
        private String type; // Visa
        private String last4; // 4242
    }
}
