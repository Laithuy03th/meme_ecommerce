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

    /**
     * [Tầng 3 - Idempotency] Key duy nhất cho mỗi lần bấm "Đặt hàng".
     * Frontend sinh UUID trước khi hiển thị trang thanh toán.
     * Nếu user bấm nhiều lần với cùng key → chỉ tạo 1 đơn hàng.
     * Nếu để null → không áp dụng idempotency check (backward compatible).
     *
     * Ví dụ frontend: idempotencyKey = crypto.randomUUID()
     */
    private String idempotencyKey;
}
