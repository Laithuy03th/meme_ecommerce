package com.example.MyWeb.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Shipping Method entity
 * Represents different shipping options (Standard, Express, Instant)
 * 
 * @author Senior Software Engineer
 */
@Entity
@Table(name = "shipping_methods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code; // "STANDARD", "EXPRESS", "INSTANT"

    @Column(nullable = false, length = 100)
    private String name; // "Giao hàng tiêu chuẩn"

    @Column(length = 255)
    private String description; // "Giao trong 3-5 ngày làm việc"

    @Column(name = "base_fee", nullable = false)
    private Double baseFee; // Base shipping fee

    @Column(name = "estimated_min_days")
    private Integer estimatedMinDays; // Min delivery days

    @Column(name = "estimated_max_days")
    private Integer estimatedMaxDays; // Max delivery days

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0; // Display order (lower = first)

    @Column(name = "icon_url")
    private String iconUrl; // Icon for frontend display

    /**
     * Get estimated delivery time as string
     */
    @Transient
    public String getEstimatedDeliveryTime() {
        if (estimatedMinDays == null || estimatedMaxDays == null) {
            return "Liên hệ shop";
        }
        if (estimatedMinDays.equals(estimatedMaxDays)) {
            return estimatedMinDays + " ngày";
        }
        return estimatedMinDays + "-" + estimatedMaxDays + " ngày";
    }

    /**
     * Check if this is express shipping
     */
    @Transient
    public boolean isExpress() {
        return "EXPRESS".equalsIgnoreCase(code) || "INSTANT".equalsIgnoreCase(code);
    }
}
