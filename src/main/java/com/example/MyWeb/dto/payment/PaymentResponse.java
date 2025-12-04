package com.example.MyWeb.dto.payment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentResponse {

    private Long orderId;

    private String paymentMethod;

    private String paymentStatus;

    private String paymentUrl; // URL để redirect đến cổng thanh toán (VNPay, Momo...)

    private Double amount;

    private String transactionId; // ID giao dịch từ payment gateway

    private String message;
}
