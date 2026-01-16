package com.example.MyWeb.model;

import com.example.MyWeb.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity to track order status changes history
 * Provides audit trail for all order status transitions
 * 
 * @author Senior Software Engineer
 * @version 1.0
 */
@Entity
@Table(name = "order_status_history", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus toStatus;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "changed_by", length = 100)
    private String changedBy; // Username, "SYSTEM", or "CUSTOMER"

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // Track IP for security

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Create a system-generated status change record
     */
    public static OrderStatusHistory systemChange(Order order, OrderStatus from, OrderStatus to, String note) {
        return OrderStatusHistory.builder()
                .order(order)
                .fromStatus(from)
                .toStatus(to)
                .note(note)
                .changedBy("SYSTEM")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create an admin-generated status change record
     */
    public static OrderStatusHistory adminChange(Order order, OrderStatus from, OrderStatus to, String admin,
            String note) {
        return OrderStatusHistory.builder()
                .order(order)
                .fromStatus(from)
                .toStatus(to)
                .note(note)
                .changedBy(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
