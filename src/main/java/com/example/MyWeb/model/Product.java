package com.example.MyWeb.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name; // Tên sản phẩm hiển thị

    @Column(nullable = false, unique = true)
    private String slug; // dung-cho-url

    @Column(name = "short_desc", columnDefinition = "TEXT")
    private String shortDesc;

    @Column(name = "long_desc", columnDefinition = "TEXT")
    private String longDesc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "base_price", nullable = false)
    private Double basePrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity; // Dành cho sản phẩm đơn giản hoặc tổng tồn kho

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE / INACTIVE / DRAFT

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants;
}
