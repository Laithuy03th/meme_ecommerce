package com.example.MyWeb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    private String response;
    private String intent;
    private String sessionId;
    private List<QuickReply> quickReplies; // Suggested quick replies
    private Map<String, Object> data; // Additional data (products, orders, etc.)
    private Boolean requiresAuth; // Does this action require authentication?
}
