package com.example.MyWeb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Đại diện cho một lượt hội thoại (user hoặc bot).
 * Dùng để lưu ngữ cảnh hội thoại ngắn hạn trong ConversationContextService.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatTurn {

    /**
     * Vai trò: "user" hoặc "model"
     */
    private String role;

    /**
     * Nội dung tin nhắn
     */
    private String content;

    /**
     * Intent đã phân loại (product, policy, order, greeting, other)
     */
    private String intent;

    /**
     * Timestamp
     */
    private long timestamp;
}
