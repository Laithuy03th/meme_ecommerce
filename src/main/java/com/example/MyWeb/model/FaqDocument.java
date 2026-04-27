package com.example.MyWeb.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", length = 100)
    private String category; // 'shipping', 'payment', 'return', 'warranty'

    // Dành cho Hibernate 6.4+ (Vector Search)
    // Yêu cầu: CREATE EXTENSION IF NOT EXISTS vector; trong PostgreSQL
    @Column(columnDefinition = "vector(3072)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    private float[] embedding;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
