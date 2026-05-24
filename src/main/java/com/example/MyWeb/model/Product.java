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

    /**
     * Optimistic Locking - JPA tự động tăng version mỗi lần update.
     * Nếu 2 transaction đọc cùng version=5, chỉ 1 transaction đầu tiên
     * lưu được. Transaction thứ 2 sẽ nhận ObjectOptimisticLockingFailureException.
     */
    @Version
    @Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
    private Long version = 0L;

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

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "average_rating")
    private Double averageRating; // Điểm đánh giá trung bình (VD: 4.5)

    @Column(name = "review_count")
    private Integer reviewCount = 0; // Số lượng đánh giá

    @Column(name = "sold_count")
    private Integer soldCount = 0; // Số lượng đã bán (for best sellers)

    @Column(name = "view_count")
    private Integer viewCount = 0; // Lượt xem (for analytics)

    @Column(name = "sku", length = 50, unique = true)
    private String sku; // Stock Keeping Unit

    @Column(name = "weight")
    private Double weight; // Khối lượng (kg) - for shipping calculation

    @Column(name = "is_featured")
    private Boolean isFeatured = false; // Sản phẩm nổi bật

    @Column(name = "video_url")
    private String videoUrl; // URL video giới thiệu sản phẩm

    /**
     * Thông số kỹ thuật theo category, lưu dạng JSON.
     * Electronics: {"screen": "6.1 inch", "ram": "8GB", ...}
     * Beauty: {"volume": "30ml", "skinType": "Da khô", ...}
     * Fashion: {"material": "100% Cotton", "style": "Casual", ...}
     * Home-Living: {"material": "Gỗ MDF", "dimensions": "60x65x120cm", ...}
     */
    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

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
