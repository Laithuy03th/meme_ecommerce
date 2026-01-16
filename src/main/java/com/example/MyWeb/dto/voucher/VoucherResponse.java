package com.example.MyWeb.dto.voucher;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherResponse {

    private Long id;
    private String code;
    private String discountType;
    private Double discountValue;
    private Double minOrderAmount;
    private Double maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean isActive;

    // New fields
    private Integer usageLimitPerUser;
    private Boolean freeShipping;
    private Double maxShippingDiscount;
    private String applicableCategoryIds;
}
