package com.example.MyWeb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryItemResponse {
    private String role; // "user" or "bot"
    private String content; // message for user, response for bot
    private String intent;
    private String sessionId;
    private LocalDateTime createdAt;
    private List<QuickReply> quickReplies;
    private Map<String, Object> data;
}
