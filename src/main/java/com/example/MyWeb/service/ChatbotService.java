package com.example.MyWeb.service;

import com.example.MyWeb.dto.ChatRequest;
import com.example.MyWeb.dto.ChatResponse;
import com.example.MyWeb.dto.ChatHistoryItemResponse;

import java.util.List;

public interface ChatbotService {

    ChatResponse processMessage(ChatRequest request);

    List<ChatHistoryItemResponse> getChatHistory(String sessionId);

    void initializeKnowledgeBase();

    List<String> getQuickStartSuggestions();
}
