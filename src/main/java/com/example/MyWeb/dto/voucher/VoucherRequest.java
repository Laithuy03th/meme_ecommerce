package com.example.MyWeb.dto.voucher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private String discountType; // PERCENT or AMOUNT

    @NotNull(message = "Discount value is required")
    private Double discountValue;

    private Double minOrderAmount;
    private Double maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
}
