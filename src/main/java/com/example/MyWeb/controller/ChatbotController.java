package com.example.MyWeb.controller;

import com.example.MyWeb.dto.ChatRequest;
import com.example.MyWeb.dto.ChatResponse;
import com.example.MyWeb.security.CustomUserDetails;
import com.example.MyWeb.service.ChatbotService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chatbot", description = "API chatbot tự động hỗ trợ khách hàng 24/7. " +
                "Chatbot có khả năng: tìm sản phẩm, tra cứu đơn hàng, hướng dẫn thanh toán, " +
                "cung cấp thông tin giao hàng và voucher.")
// CORS already configured globally in SecurityConfig.java
public class ChatbotController {

        private final ChatbotService chatbotService;

        @PostMapping("/message")
        @Operation(summary = "Gửi tin nhắn đến chatbot", description = "**Chức năng chính:** Xử lý tin nhắn từ user và trả về response tự động.\n\n"
                        +
                        "**Intents được hỗ trợ:**\n" +
                        "- `greeting`: Chào hỏi (\"xin chào\", \"hello\")\n" +
                        "- `product_inquiry`: Tìm sản phẩm (\"tìm áo\", \"có laptop không\")\n" +
                        "- `order_tracking`: Tra cứu đơn hàng (\"đơn 123\", \"kiểm tra đơn hàng\")\n" +
                        "- `payment_info`: Thông tin thanh toán (\"thanh toán như thế nào\")\n" +
                        "- `shipping_info`: Chính sách giao hàng (\"giao hàng bao lâu\")\n" +
                        "- `voucher_info`: Thông tin voucher (\"có voucher gì\")\n" +
                        "- `contact`: Liên hệ support (\"liên hệ\", \"hotline\")\n" +
                        "- `fallback`: Không hiểu câu hỏi\n\n" +
                        "**Lưu ý:** \n" +
                        "- SessionId sẽ tự động tạo nếu không có\n" +
                        "- UserId tự động lấy từ authentication nếu user đã đăng nhập\n" +
                        "- Một số tính năng (tra cứu đơn hàng) yêu cầu authentication")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Thành công - Chatbot trả về response", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatResponse.class), examples = @ExampleObject(name = "Greeting Response", value = """
                                        {
                                          "response": "Xin chào! 👋 Tôi là trợ lý ảo của MyWeb. Tôi có thể giúp gì cho bạn?",
                                          "intent": "greeting",
                                          "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                          "quickReplies": [
                                            {
                                              "label": "Tìm sản phẩm",
                                              "value": "tìm sản phẩm",
                                              "icon": "🔍"
                                            },
                                            {
                                              "label": "Kiểm tra đơn hàng",
                                              "value": "kiểm tra đơn hàng",
                                              "icon": "📦"
                                            }
                                          ],
                                          "data": null,
                                          "requiresAuth": false
                                        }
                                        """))),
                        @ApiResponse(responseCode = "400", description = "Bad Request - Request không hợp lệ")
        })
        public ResponseEntity<ChatResponse> sendMessage(
                        @Valid @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Request gửi tin nhắn đến chatbot", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatRequest.class), examples = {
                                        @ExampleObject(name = "Greeting", value = """
                                                        {
                                                          "message": "xin chào",
                                                          "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                          "userId": null
                                                        }
                                                        """),
                                        @ExampleObject(name = "Product Search", value = """
                                                        {
                                                          "message": "tìm áo sơ mi",
                                                          "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                          "userId": null
                                                        }
                                                        """),
                                        @ExampleObject(name = "Order Tracking", value = """
                                                        {
                                                          "message": "đơn 123",
                                                          "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                          "userId": 1
                                                        }
                                                        """)
                        })) ChatRequest request) {

                // Generate session ID if not provided
                if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
                        request.setSessionId(UUID.randomUUID().toString());
                }

                // Get user ID from authentication if available
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                        try {
                                Object principal = auth.getPrincipal();
                                if (principal instanceof CustomUserDetails) {
                                        CustomUserDetails userDetails = (CustomUserDetails) principal;
                                        request.setUserId(userDetails.getId());
                                        log.debug("User authenticated: userId={}", userDetails.getId());
                                }
                        } catch (Exception e) {
                                log.warn("Failed to extract userId from authentication", e);
                                // Continue as anonymous user
                        }
                }

                ChatResponse response = chatbotService.processMessage(request);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/history/{sessionId}")
        @Operation(summary = "Lấy lịch sử chat của một session", description = "Trả về danh sách các tin nhắn bot đã trả lời trong session.\n\n"
                        +
                        "**Use case:** Hiển thị lại conversation history khi user refresh page hoặc quay lại chat.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Thành công - Trả về lịch sử chat", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatResponse.class), examples = @ExampleObject(name = "Chat History", value = """
                                        [
                                          {
                                            "response": "Xin chào! Tôi có thể giúp gì cho bạn?",
                                            "intent": "greeting",
                                            "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                            "quickReplies": null,
                                            "data": null,
                                            "requiresAuth": null
                                          },
                                          {
                                            "response": "Tìm thấy 5 sản phẩm phù hợp với 'áo':",
                                            "intent": "product_inquiry",
                                            "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                            "quickReplies": null,
                                            "data": null,
                                            "requiresAuth": null
                                          }
                                        ]
                                        """)))
        })
        public ResponseEntity<List<com.example.MyWeb.model.ChatMessage>> getChatHistory(
                        @Parameter(description = "Session ID (UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") @PathVariable String sessionId) {
                List<com.example.MyWeb.model.ChatMessage> history = chatbotService.getChatHistory(sessionId);
                return ResponseEntity.ok(history);
        }

        @GetMapping("/suggestions")
        @Operation(summary = "Lấy gợi ý câu hỏi nhanh", description = "Trả về danh sách các câu hỏi gợi ý để user có thể click nhanh.\n\n"
                        +
                        "**Use case:** Hiển thị quick start suggestions khi mở chatbot lần đầu.")
        @ApiResponse(responseCode = "200", description = "Thành công - Trả về danh sách gợi ý", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Suggestions", value = """
                        [
                          "Tìm sản phẩm",
                          "Kiểm tra đơn hàng",
                          "Thanh toán như thế nào?",
                          "Chính sách giao hàng",
                          "Voucher giảm giá"
                        ]
                        """)))
        public ResponseEntity<List<String>> getQuickSuggestions() {
                List<String> suggestions = chatbotService.getQuickStartSuggestions();
                return ResponseEntity.ok(suggestions);
        }

        @PostMapping("/init")
        @Operation(summary = "Khởi tạo knowledge base", description = "**Admin only** - Khởi tạo knowledge base với các intents mặc định.\n\n"
                        +
                        "**Lưu ý:** \n" +
                        "- Chỉ cần chạy 1 lần khi setup lần đầu\n" +
                        "- Nếu đã có data sẽ không insert lại\n" +
                        "- Tự động chạy qua @PostConstruct khi start app")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Thành công - Knowledge base đã được khởi tạo", content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "Knowledge base initialized successfully")))
        })
        public ResponseEntity<String> initializeKnowledgeBase() {
                chatbotService.initializeKnowledgeBase();
                return ResponseEntity.ok("Knowledge base initialized successfully");
        }
}
