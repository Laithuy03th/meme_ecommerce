package com.example.MyWeb.service;

import com.example.MyWeb.dto.ChatRequest;
import com.example.MyWeb.dto.ChatResponse;
import com.example.MyWeb.model.ChatMessage;
import com.example.MyWeb.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service riêng để lưu lịch sử chat vào DB.
 *
 * Lý do tách ra: @Transactional(REQUIRES_NEW) trên protected method trong cùng class
 * sẽ bị Spring AOP proxy bỏ qua (self-invocation không qua proxy).
 * Bằng cách inject service này vào ChatbotServiceImpl, Spring proxy hoạt động đúng,
 * đảm bảo transaction lưu chat hoàn toàn độc lập với bất kỳ rollback nào bên ngoài.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    /**
     * Lưu cặp user-message + bot-response vào DB trong một transaction độc lập.
     * REQUIRES_NEW: luôn tạo transaction mới, không bị ảnh hưởng bởi
     * transaction cha dù có lỗi rollback hay không.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveChatMessage(ChatRequest request, ChatResponse response) {
        try {
            // Lưu tin nhắn user
            chatMessageRepository.save(ChatMessage.builder()
                    .sessionId(request.getSessionId())
                    .message(request.getMessage())
                    .response("")   // user turn: response rỗng
                    .messageType(ChatMessage.MessageType.USER)
                    .userId(request.getUserId())
                    .build());

            // Serialize payload
            String responseDataJson = null;
            if (response.getData() != null) {
                responseDataJson = objectMapper.writeValueAsString(response.getData());
            }
            String quickRepliesJson = null;
            if (response.getQuickReplies() != null) {
                quickRepliesJson = objectMapper.writeValueAsString(response.getQuickReplies());
            }

            // Lưu phản hồi bot — KHÔNG lưu lại message của user vào bot record
            chatMessageRepository.save(ChatMessage.builder()
                    .sessionId(request.getSessionId())
                    .message("")    // bot turn: message rỗng (fix data model dư thừa)
                    .response(response.getResponse() != null ? response.getResponse() : "")
                    .responseData(responseDataJson)
                    .quickReplies(quickRepliesJson)
                    .messageType(ChatMessage.MessageType.BOT)
                    .intent(response.getIntent())
                    .userId(request.getUserId())
                    .build());

        } catch (Exception e) {
            // Không để lỗi DB làm hỏng response trả về cho user
            log.error("[ChatHistory] Failed to save chat message to DB: {}", e.getMessage());
        }
    }
}
