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

    // Phương thức vận chuyển (Standard/Express)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_method_id")
    private ShippingMethod shippingMethod;

    // PENDING / PAID / SHIPPED / COMPLETED / CANCELED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.example.MyWeb.model.enums.OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "shipping_fee", nullable = false)
    private Double shippingFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private com.example.MyWeb.model.enums.PaymentMethod paymentMethod; // COD / VNPAY / MOMO...

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private com.example.MyWeb.model.enums.PaymentStatus paymentStatus; // UNPAID / PAID / FAILED

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "voucher_code")
    private String voucherCode;

    @Column(name = "discount_amount")
    private Double discountAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;
}
