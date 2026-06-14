package com.example.MyWeb.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {

    @NotNull(message = "Address is required")
    private Long addressId;

    @NotNull(message = "Shipping method is required")
    private Long shippingMethodId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String note;

    private String voucherCode;
    private java.util.List<Long> selectedCartItemIds;

    private String idempotencyKey;
}
