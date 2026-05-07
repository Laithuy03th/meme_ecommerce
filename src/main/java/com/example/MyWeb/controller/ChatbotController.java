package com.example.MyWeb.controller;

import com.example.MyWeb.dto.ChatRequest;
import com.example.MyWeb.dto.ChatResponse;
import com.example.MyWeb.dto.ChatHistoryItemResponse;
import com.example.MyWeb.security.CustomUserDetails;
import com.example.MyWeb.service.ChatbotService;
import com.example.MyWeb.service.ConversationContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chatbot", description = "API chatbot tự động hỗ trợ khách hàng 24/7.")
public class ChatbotController {

        private final ChatbotService chatbotService;
        private final ConversationContextService conversationContextService;

        // =========================================================================
        // Core: Send Message
        // =========================================================================

        @PostMapping("/message")
        @Operation(summary = "Gửi tin nhắn đến chatbot")
        public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
                if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
                        request.setSessionId(UUID.randomUUID().toString());
                }
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                        try {
                                if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                                        request.setUserId(userDetails.getId());
                                }
                        } catch (Exception e) {
                                log.warn("Failed to extract userId from authentication", e);
                        }
                }
                ChatResponse response = chatbotService.processMessage(request);
                return ResponseEntity.ok(response);
        }

        // =========================================================================
        // History: Session List & Messages
        // =========================================================================

        /**
         * GET /api/chatbot/history/{sessionId}
         * Lấy toàn bộ tin nhắn trong một session cụ thể.
         */
        @GetMapping("/history/{sessionId}")
        @Operation(
                summary = "Lấy lịch sử chat của một session",
                description = "Trả về danh sách tin nhắn user + bot trong session theo thứ tự thời gian.")
        public ResponseEntity<List<ChatHistoryItemResponse>> getChatHistory(
                        @Parameter(description = "Session ID (UUID)") @PathVariable String sessionId) {
                List<ChatHistoryItemResponse> history = chatbotService.getChatHistory(sessionId);
                return ResponseEntity.ok(history);
        }

        // =========================================================================
        // Utilities
        // =========================================================================

        @GetMapping("/suggestions")
        @Operation(summary = "Lấy gợi ý câu hỏi nhanh")
        public ResponseEntity<List<String>> getQuickSuggestions() {
                return ResponseEntity.ok(chatbotService.getQuickStartSuggestions());
        }

        @PostMapping("/init")
        @Operation(summary = "Khởi tạo knowledge base (admin)")
        public ResponseEntity<String> initializeKnowledgeBase() {
                chatbotService.initializeKnowledgeBase();
                return ResponseEntity.ok("Knowledge base initialized successfully");
        }

        // =========================================================================
        // Helpers
        // =========================================================================

        private Long extractUserId() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                        try {
                                if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                                        return userDetails.getId();
                                }
                        } catch (Exception ignored) {}
                }
                return null;
        }
}
