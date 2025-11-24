package com.example.MyWeb.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chủ đơn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Địa chỉ giao hàng (snapshot)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    // PENDING / PAID / SHIPPED / COMPLETED / CANCELED
    @Column(nullable = false)
    private String status;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "shipping_fee", nullable = false)
    private Double shippingFee;

    @Column(name = "payment_method")
    private String paymentMethod; // COD / VNPAY / MOMO...

    @Column(name = "payment_status")
    private String paymentStatus; // UNPAID / PAID / FAILED

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;
}
