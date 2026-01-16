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
    private Long shippingMethodId; // Shipping method selection (Standard/Express)

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // COD, VNPAY, MOMO...

    private String note;

    private String voucherCode; // Optional voucher code

    // New: Support for Partial Checkout (Buying selected items only)
    // If null or empty, behavior defaults to "Buy All" (or throw error depending on
    // strictness)
    private java.util.List<Long> selectedCartItemIds;
}
