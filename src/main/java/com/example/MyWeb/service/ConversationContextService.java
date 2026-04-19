package com.example.MyWeb.service;

import com.example.MyWeb.dto.ChatTurn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.example.MyWeb.model.ChatMessage;

/**
 * Quản lý ngữ cảnh hội thoại ngắn hạn cho mỗi session.
 *
 * Dùng in-memory ConcurrentHashMap (phù hợp cho đồ án single-node).
 * Có TTL tự động dọn dẹp session cũ sau 30 phút.
 *
 * Đây là thành phần cốt lõi tạo ra cảm giác "chatbot thông minh":
 * - User: "Laptop dưới 15 triệu?" → Bot gợi ý 3 model
 * - User: "Con nào pin hơn?" → Bot biết đang hỏi về 3 model vừa gợi ý
 */
@Service
@Slf4j
public class ConversationContextService {

    @Value("${chatbot.context.max-turns:8}")
    private int maxTurns;

    @Value("${chatbot.context.ttl-minutes:30}")
    private int ttlMinutes;

    // sessionId → Deque<ChatTurn> (FIFO bounded queue)
    private final ConcurrentHashMap<String, Deque<ChatTurn>> contextStore = new ConcurrentHashMap<>();

    // sessionId → last access timestamp (millis)
    private final ConcurrentHashMap<String, Long> lastAccessTime = new ConcurrentHashMap<>();

    /**
     * Lấy lịch sử hội thoại của một session (sorted by time, oldest first).
     */
    public List<ChatTurn> getHistory(String sessionId) {
        lastAccessTime.put(sessionId, System.currentTimeMillis());
        Deque<ChatTurn> deque = contextStore.get(sessionId);
        if (deque == null) return List.of();
        return new ArrayList<>(deque); // Return copy for thread safety
    }

    /**
     * Thêm lượt user vào context của session.
     */
    public void addUserTurn(String sessionId, String userMessage) {
        addTurn(sessionId, ChatTurn.builder()
                .role("user")
                .content(userMessage)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    /**
     * Thêm lượt bot vào context của session.
     *
     * @param response  Câu trả lời của bot
     * @param intent    Intent đã phân loại
     */
    public void addBotTurn(String sessionId, String response, String intent) {
        addTurn(sessionId, ChatTurn.builder()
                .role("model")
                .content(response)
                .intent(intent)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    /**
     * Lấy intent của lượt trước để hỗ trợ continuity (VD: follow-up về product)
     */
    public String getLastIntent(String sessionId) {
        List<ChatTurn> history = getHistory(sessionId);
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatTurn turn = history.get(i);
            if ("model".equals(turn.getRole()) && turn.getIntent() != null) {
                return turn.getIntent();
            }
        }
        return null;
    }

    /**
     * Xóa context của một session (khi user reset chat).
     */
    public void clearSession(String sessionId) {
        contextStore.remove(sessionId);
        lastAccessTime.remove(sessionId);
        log.debug("Cleared context for session: {}", sessionId);
    }

    /**
     * Dọn dẹp session hết hạn mỗi 5 phút.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void evictExpiredSessions() {
        long now = System.currentTimeMillis();
        long ttlMs = ttlMinutes * 60 * 1000L;
        int evicted = 0;

        for (String sessionId : new ArrayList<>(lastAccessTime.keySet())) {
            Long lastAccess = lastAccessTime.get(sessionId);
            if (lastAccess != null && (now - lastAccess) > ttlMs) {
                contextStore.remove(sessionId);
                lastAccessTime.remove(sessionId);
                evicted++;
            }
        }

        if (evicted > 0) {
            log.info("Evicted {} expired chat sessions", evicted);
        }
    }

    // =========================================================================
    // Phục hồi từ DB (Cross-session Persistence)
    // =========================================================================

    /**
     * Phục hồi in-memory context từ dữ liệu DB (khi user quay lại vào hôm sau hoặc sau khi server restart).
     * Chỉ nạp tối đa `maxTurns` tin nhắn gần nhất để tránh tràn RAM và token limit.
     */
    public void restoreFromDb(String sessionId, List<ChatMessage> dbHistory) {
        if (dbHistory == null || dbHistory.isEmpty()) return;
        
        // Chỉ lấy những tin nhắn gần nhất (tránh nhồi toàn bộ lịch sử nếu họ đã chat hàng trăm câu)
        int keepCount = Math.min(dbHistory.size(), maxTurns);
        List<ChatMessage> recentMessages = dbHistory.subList(dbHistory.size() - keepCount, dbHistory.size());
        
        for (ChatMessage msg : recentMessages) {
            String role = msg.getMessageType() == ChatMessage.MessageType.USER ? "user" : "model";
            String content = role.equals("user") ? msg.getMessage() : msg.getResponse();
            
            // Bỏ qua tin nhắn rỗng (ví dụ: dummy DB entries)
            if (content == null || content.trim().isEmpty()) continue;
            
            addTurn(sessionId, ChatTurn.builder()
                    .role(role)
                    .content(content)
                    .intent(msg.getIntent())
                    .timestamp(msg.getCreatedAt() != null ? 
                            msg.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 
                            System.currentTimeMillis())
                    .build());
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void addTurn(String sessionId, ChatTurn turn) {
        lastAccessTime.put(sessionId, System.currentTimeMillis());
        contextStore.compute(sessionId, (key, existing) -> {
            if (existing == null) {
                existing = new ArrayDeque<>();
            }
            existing.addLast(turn);

            // Giữ tối đa maxTurns, xóa turn cũ nhất (FIFO)
            while (existing.size() > maxTurns) {
                existing.pollFirst();
            }
            return existing;
        });
    }
}
