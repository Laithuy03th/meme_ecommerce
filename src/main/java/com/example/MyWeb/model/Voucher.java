package com.example.MyWeb.model;

import com.example.MyWeb.model.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "discount_type")
    private DiscountType discountType;

    @Column(nullable = false, name = "discount_value")
    private Double discountValue; // Giá trị giảm (% hoặc số tiền)

    @Column(name = "min_order_amount")
    private Double minOrderAmount; // Đơn tối thiểu để áp dụng

    @Column(name = "max_discount_amount")
    private Double maxDiscountAmount; // Giảm tối đa (cho PERCENT)

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "usage_limit")
    private Integer usageLimit; // Giới hạn số lần dùng (null = unlimited)

    @Column(name = "used_count")
    @Builder.Default
    private Integer usedCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "free_shipping")
    @Builder.Default
    private Boolean freeShipping = false;

    @Column(name = "usage_limit_per_user")
    @Builder.Default
    private Integer usageLimitPerUser = 1;

    @Column(name = "applicable_category_ids")
    private String applicableCategoryIds;

    @Column(name = "max_shipping_discount")
    private Double maxShippingDiscount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        if (!isActive)
            return false;

        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate))
            return false;
        if (endDate != null && now.isAfter(endDate))
            return false;

        if (usageLimit != null && usedCount >= usageLimit)
            return false;

        return true;
    }

    public Double calculateDiscount(Double orderAmount) {
        if (minOrderAmount != null && orderAmount < minOrderAmount) {
            return 0.0;
        }

        double discount;
        if (discountType == DiscountType.PERCENT) {
            discount = orderAmount * (discountValue / 100.0);
            if (maxDiscountAmount != null && discount > maxDiscountAmount) {
                discount = maxDiscountAmount;
            }
        } else {
            discount = discountValue;
        }

        return Math.min(discount, orderAmount); // Không giảm quá tổng đơn
    }
}
