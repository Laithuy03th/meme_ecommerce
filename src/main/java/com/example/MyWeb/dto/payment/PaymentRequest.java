package com.example.MyWeb.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {

    @NotNull
    private Long orderId;

    private String returnUrl; // URL để redirect sau khi thanh toán

    private String cancelUrl; // URL để redirect nếu hủy thanh toán
}
