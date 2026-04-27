package com.example.MyWeb.controller;

import com.example.MyWeb.service.FaqIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin API cho quản lý FAQ và chatbot.
 * Bảo vệ bằng ADMIN role — không public.
 */
@RestController
@RequestMapping("/api/v1/admin/chatbot")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatbotController {

    private final FaqIndexingService faqIndexingService;

    /**
     * Trigger tạo embedding cho tất cả FAQ document chưa có embedding.
     * Gọi bằng Postman/curl (POST request) sau khi seed data.
     *
     * curl -X POST http://localhost:8080/api/v1/admin/chatbot/faq/index-embeddings \
     *      -H "Authorization: Bearer <admin_token>"
     */
    @PostMapping("/faq/index-embeddings")
    public ResponseEntity<Map<String, Object>> indexFaqEmbeddings() {
        log.info("[AdminChatbot] Starting FAQ embedding indexing...");
        int indexed = faqIndexingService.indexMissingEmbeddings();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "indexed", indexed,
                "message", "Đã index embedding cho " + indexed + " FAQ document(s)."
        ));
    }
}
