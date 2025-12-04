package com.example.MyWeb.dto.payment;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class PaymentCallbackRequest {

    private Long orderId;

    private String transactionId;

    private String status; // SUCCESS / FAILED / CANCELLED

    private Map<String, String> additionalData; // Dữ liệu bổ sung từ payment gateway
}
