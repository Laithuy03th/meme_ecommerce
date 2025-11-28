package com.example.MyWeb.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Clothes, Dresses, Shoes...

    @Column(nullable = false, unique = true)
    private String slug; // clothes, dresses, shoes

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent; // optional cho future

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE / INACTIVE

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
