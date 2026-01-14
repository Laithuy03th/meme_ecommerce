package com.example.MyWeb.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chatbot_knowledge")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String intent; // Tên intent (e.g., "greeting", "product_inquiry", "order_tracking")

    @ElementCollection
    @CollectionTable(name = "chatbot_patterns", joinColumns = @JoinColumn(name = "knowledge_id"))
    @Column(name = "pattern")
    private List<String> patterns; // Các mẫu câu hỏi

    @ElementCollection
    @CollectionTable(name = "chatbot_responses", joinColumns = @JoinColumn(name = "knowledge_id"))
    @Column(name = "response", length = 1000)
    private List<String> responses; // Các câu trả lời có thể

    @Column(name = "requires_data")
    private Boolean requiresData = false; // Có cần lấy data từ DB không?

    @Column(name = "data_source")
    private String dataSource; // Source để lấy data (products, orders, etc.)

    @Column(name = "priority")
    private Integer priority = 0; // Độ ưu tiên (số cao hơn = ưu tiên cao hơn)

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
