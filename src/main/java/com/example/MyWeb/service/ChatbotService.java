package com.example.MyWeb.service;

import com.example.MyWeb.dto.ChatRequest;
import com.example.MyWeb.dto.ChatResponse;

import java.util.List;

public interface ChatbotService {

    /**
     * Process incoming chat message and generate response
     */
    ChatResponse processMessage(ChatRequest request);

    /**
     * Get chat history for a session
     */
    List<ChatResponse> getChatHistory(String sessionId);

    /**
     * Initialize chatbot knowledge base
     */
    void initializeKnowledgeBase();

    /**
     * Get quick start suggestions
     */
    List<String> getQuickStartSuggestions();
}
