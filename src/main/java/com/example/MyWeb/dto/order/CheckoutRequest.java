package com.example.MyWeb.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {

    @NotNull
    private Long addressId;

    @NotBlank
    private String paymentMethod; // COD, VNPAY, MOMO...

    private String note;

    private String voucherCode; // Optional voucher code
}
