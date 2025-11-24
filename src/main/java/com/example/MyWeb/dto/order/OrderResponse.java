package com.example.MyWeb.dto.order;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;

    private String status;
    private String paymentStatus;
    private String paymentMethod;

    private Double totalAmount;
    private Double shippingFee;
    private String note;

    private LocalDateTime createdAt;

    private List<OrderItemResponse> items;
}
