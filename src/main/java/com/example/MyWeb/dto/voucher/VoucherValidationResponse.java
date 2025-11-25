package com.example.MyWeb.dto.voucher;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherValidationResponse {

    private Boolean valid;
    private String message;
    private Double discountAmount;
    private VoucherResponse voucher;
}
