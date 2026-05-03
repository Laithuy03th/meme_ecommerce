package com.example.MyWeb.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId; // Session ID để track conversation

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    @Column(name = "quick_replies", columnDefinition = "TEXT")
    private String quickReplies;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    private String intent; // Intent được phát hiện (product_inquiry, order_status, etc.)

    @Column(name = "user_id")
    private Long userId; // Optional - nếu user đã đăng nhập

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum MessageType {
        USER,
        BOT
    }
}
