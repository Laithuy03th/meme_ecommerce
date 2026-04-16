package com.example.MyWeb.service;

import com.example.MyWeb.dto.ChatRequest;
import com.example.MyWeb.dto.ChatResponse;
import com.example.MyWeb.dto.ChatTurn;
import com.example.MyWeb.dto.QuickReply;
import com.example.MyWeb.model.ChatMessage;
import com.example.MyWeb.model.FaqDocument;
import com.example.MyWeb.model.Order;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.repository.ChatMessageRepository;
import com.example.MyWeb.repository.ChatbotKnowledgeRepository;
import com.example.MyWeb.repository.OrderRepository;
import com.example.MyWeb.repository.ProductRepository;
import com.example.MyWeb.repository.spec.ProductSpecifications;
import com.example.MyWeb.service.impl.GeminiLlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered Chatbot Service — Tuần 2 Implementation (RAG Integration)
 *
 * Pipeline:
 * 1. Load conversation context (in-memory)
 * 2. LLM classify intent (Gemini gemini-1.5-flash)
 * 3. Route theo intent:
 *    - product → extract constraints → query DB → LLM diễn đạt
 *    - policy  → hardcoded + LLM (RAG sẽ thêm ở Tuần 2-3)
 *    - order   → query DB → format response
 *    - greeting → welcome message
 * 4. Update context + save to DB
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatbotKnowledgeRepository knowledgeRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final LlmService llmService;
    private final RagService ragService;
    private final ConversationContextService contextService;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // Public API (implements ChatbotService interface)
    // =========================================================================

    @Override
    @Transactional
    public ChatResponse processMessage(ChatRequest request) {
        String sessionId = request.getSessionId();
        String userMessage = request.getMessage().trim();

        log.info("[Chatbot] Session={} | User: {}", sessionId.substring(0, 8), userMessage);

        // 1. Load conversation context
        List<ChatTurn> history = contextService.getHistory(sessionId);

        // 2. Classify intent via Gemini LLM
        String intent;
        try {
            intent = llmService.classifyIntent(userMessage, history);
        } catch (Exception e) {
            log.error("LLM classification failed, using fallback", e);
            intent = "other";
        }

        log.info("[Chatbot] Intent: '{}'", intent);

        // 3. Update context with user turn
        contextService.addUserTurn(sessionId, userMessage);

        // 4. Generate response based on intent
        ChatResponse response = routeAndRespond(intent, userMessage, history, request);

        // 5. Update context with bot response
        contextService.addBotTurn(sessionId, response.getResponse(), intent);

        // 6. Save to DB (async-safe)
        saveChatMessage(request, response);

        return response;
    }

    @Override
    public List<ChatMessage> getChatHistory(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Override
    public List<String> getQuickStartSuggestions() {
        return Arrays.asList(
                "Tìm sản phẩm",
                "Kiểm tra đơn hàng",
                "Chính sách giao hàng",
                "Đổi trả hàng như thế nào?",
                "Có voucher gì không?");
    }

    @PostConstruct
    @Override
    public void initializeKnowledgeBase() {
        // Knowledge base cũ vẫn giữ để backward-compat, nhưng không còn là engine chính
        if (knowledgeRepository.count() > 0) {
            log.info("Knowledge base already initialized (legacy)");
            return;
        }
        log.info("Knowledge base is empty, skip legacy init (using LLM now)");
    }

    // =========================================================================
    // Intent Routing
    // =========================================================================

    private ChatResponse routeAndRespond(String intent, String userMessage,
                                          List<ChatTurn> history, ChatRequest request) {
        ChatResponse.ChatResponseBuilder builder = ChatResponse.builder()
                .intent(intent)
                .sessionId(request.getSessionId());

        return switch (intent) {
            case "greeting" -> handleGreeting(builder, userMessage, history);
            case "product"  -> handleProductSearch(builder, userMessage, history);
            case "policy"   -> handlePolicyQuestion(builder, userMessage, history);
            case "order"    -> handleOrderTracking(builder, userMessage, history, request.getUserId());
            default         -> handleOther(builder, userMessage, history);
        };
    }

    // =========================================================================
    // Handler: Greeting
    // =========================================================================

    private ChatResponse handleGreeting(ChatResponse.ChatResponseBuilder builder,
                                         String userMessage, List<ChatTurn> history) {
        String systemPrompt = """
                === VAI TRÒ ===
                Bạn đang là trợ lý AI của MemeShop - cửa hàng thương mại điện tử.
                Hãy chào hỏi thân thiện và giới thiệu ngắn gọn những gì bạn có thể giúp:
                - Tìm kiếm và tư vấn sản phẩm bằng ngôn ngữ tự nhiên
                - Tra cứu đơn hàng theo mã hoặc tài khoản
                - Giải đáp chính sách giao hàng, đổi trả, bảo hành
                
                Giữ câu trả lời ngắn gọn (tối đa 3-4 câu), dùng 1-2 emoji.
                """;

        String response = llmService.generateResponse(systemPrompt, userMessage, history);

        return builder
                .response(response)
                .quickReplies(Arrays.asList(
                        QuickReply.builder().label("🔍 Tìm sản phẩm").value("tôi muốn tìm sản phẩm").icon("🔍").build(),
                        QuickReply.builder().label("📦 Đơn hàng của tôi").value("xem đơn hàng của tôi").icon("📦").build(),
                        QuickReply.builder().label("🚚 Chính sách giao hàng").value("chính sách giao hàng như thế nào").icon("🚚").build(),
                        QuickReply.builder().label("🔄 Đổi trả hàng").value("chính sách đổi trả hàng").icon("🔄").build()))
                .build();
    }

    // =========================================================================
    // Handler: Product Search (quan trọng nhất)
    // =========================================================================

    private ChatResponse handleProductSearch(ChatResponse.ChatResponseBuilder builder,
                                              String userMessage, List<ChatTurn> history) {
        // 1. Kiểm tra follow-up: "con nào pin hơn?" — dựa vào context
        boolean isFollowUp = isProductFollowUp(userMessage, history);

        // 2. LLM extract search constraints
        String constraintsJson;
        try {
            constraintsJson = llmService.extractProductConstraints(userMessage, history);
            log.info("[ProductSearch] Constraints JSON: {}", constraintsJson);
        } catch (Exception e) {
            log.error("Failed to extract product constraints", e);
            constraintsJson = "{}";
        }

        // 3. Parse constraints
        String keyword = null;
        String categorySlug = null;
        String brand = null;
        Double maxPrice = null;
        Double minPrice = null;
        Double minRating = null;

        try {
            JsonNode constraints = objectMapper.readTree(constraintsJson);
            if (!constraints.path("keyword").isNull()) {
                keyword = constraints.path("keyword").asText(null);
            }
            if (!constraints.path("categorySlug").isNull()) {
                categorySlug = constraints.path("categorySlug").asText(null);
            }
            if (!constraints.path("brand").isNull()) {
                brand = constraints.path("brand").asText(null);
            }
            if (!constraints.path("maxPrice").isNull() && constraints.path("maxPrice").isNumber()) {
                maxPrice = constraints.path("maxPrice").asDouble();
            }
            if (!constraints.path("minPrice").isNull() && constraints.path("minPrice").isNumber()) {
                minPrice = constraints.path("minPrice").asDouble();
            }
            if (!constraints.path("minRating").isNull() && constraints.path("minRating").isNumber()) {
                minRating = constraints.path("minRating").asDouble();
            }
            
            // Xử lý isFollowUp nếu đang ở bối cảnh trước đó
            if (constraints.path("isFollowUp").asBoolean(false) && keyword == null) {
                // Nếu là follow-up mà ko có keyword, có thể khách đang hỏi tiếp về kết quả cũ
                // Ta có thể giữ lại keyword trước đó từ context (ở đây đơn giản hóa)
                log.info("Follow-up detected: trying to maintain product context");
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse constraints JSON, using keyword fallback: {}", constraintsJson);
            keyword = userMessage;
        }

        // 4. Query Database using ProductSpecifications
        List<Product> products = searchProductsAdvanced(keyword, categorySlug, minPrice, maxPrice, brand, minRating, 5);

        if (products.isEmpty()) {
            // Không tìm thấy — hỏi lại
            String clarifyResponse = llmService.generateResponse(
                    "Không tìm thấy sản phẩm phù hợp trong DB. Hãy xin lỗi ngắn gọn và hỏi lại yêu cầu cụ thể hơn.",
                    userMessage, history);
            return builder.response(clarifyResponse)
                    .quickReplies(List.of(
                            QuickReply.builder().label("Xem tất cả sản phẩm").value("xem tất cả sản phẩm").build(),
                            QuickReply.builder().label("Tìm lại").value("tôi muốn tìm sản phẩm khác").build()))
                    .build();
        }

        // 5. Build product data cho FE render cards
        List<Map<String, Object>> productData = products.stream().map(p -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", p.getId());
            data.put("name", p.getName());
            data.put("price", p.getBasePrice());
            data.put("imageUrl", p.getThumbnailUrl());
            data.put("slug", p.getSlug());
            data.put("brand", p.getBrand());
            data.put("rating", p.getAverageRating());
            return data;
        }).collect(Collectors.toList());

        // 6. LLM tổng hợp câu trả lời tự nhiên
        String productListText = products.stream()
                .map(p -> String.format("- %s (%s): %.0f VNĐ",
                        p.getName(),
                        p.getBrand() != null ? p.getBrand() : "N/A",
                        p.getBasePrice()))
                .collect(Collectors.joining("\n"));

        String systemPrompt = String.format("""
                === NHIỆM VỤ ===
                Tư vấn sản phẩm cho khách hàng dựa trên danh sách tìm được từ database.
                Nêu bật 1-2 điểm nổi bật phù hợp với nhu cầu. Ngắn gọn, thân thiện.
                Kết thúc bằng gợi ý xem chi tiết hoặc hỏi thêm về nhu cầu.
                
                === DANH SÁCH SẢN PHẨM TÌM ĐƯỢC ===
                %s
                
                === YÊU CẦU CỦA KHÁCH ===
                (Hãy đề cập đến yêu cầu này trong câu trả lời)
                """, productListText);

        String response = llmService.generateResponse(systemPrompt, userMessage, history);

        // 7. Build quick replies động
        List<QuickReply> quickReplies = new ArrayList<>();
        if (products.size() == 5) {
            quickReplies.add(QuickReply.builder().label("Xem thêm kết quả").value("cho xem thêm sản phẩm tương tự").build());
        }
        quickReplies.add(QuickReply.builder().label("So sánh").value("so sánh các sản phẩm này").build());
        quickReplies.add(QuickReply.builder().label("Tìm loại khác").value("tôi muốn tìm loại sản phẩm khác").build());

        return builder
                .response(response)
                .data(Map.of("products", productData))
                .quickReplies(quickReplies)
                .build();
    }

    // =========================================================================
    // Handler: Policy/FAQ (Tuần 2: RAG Integration)
    // =========================================================================

    private ChatResponse handlePolicyQuestion(ChatResponse.ChatResponseBuilder builder,
                                               String userMessage, List<ChatTurn> history) {
        
        // 1. RAG Retrieve: Tìm kiếm tài liệu FAQ phù hợp nhất từ pgvector
        List<FaqDocument> relevantDocs = ragService.retrieveRelevantContext(userMessage, 3);
        
        // 2. Build Context String
        String policyContext = "(Rất tiếc, hiện tại không tìm thấy tài liệu chính sách phù hợp. Hãy trả lời dựa trên kiến thức chung hợp lý nhất của một cửa hàng điện tử, và khuyên khách hàng liên hệ hotline.)";
        
        if (!relevantDocs.isEmpty()) {
            policyContext = ragService.buildContextString(relevantDocs);
        }

        String systemPrompt = String.format("""
                === NGỮ CẢNH CHÍNH SÁCH (Tài liệu từ hệ thống RAG) ===
                %s
                
                === HƯỚNG DẪN ===
                1. Hãy đóng vai trợ lý AI của MemeShop.
                2. CHỈ sử dụng thông tin từ 'NGỮ CẢNH CHÍNH SÁCH' ở trên để trả lời câu hỏi.
                3. Tuyệt đối KHÔNG BỊA ĐẶT chính sách.
                4. Nếu câu hỏi không được đề cập trong ngữ cảnh, hãy xin lỗi và đề nghị liên hệ support@myweb.com.
                5. Trả lời ngắn gọn, thân thiện, dùng bullet points.
                """, policyContext);

        String response = llmService.generateResponse(systemPrompt, userMessage, history);

        return builder
                .response(response)
                .quickReplies(Arrays.asList(
                        QuickReply.builder().label("🔄 Đổi trả").value("tôi muốn đổi trả hàng").build(),
                        QuickReply.builder().label("🚚 Phí ship").value("phí giao hàng bao nhiêu").build(),
                        QuickReply.builder().label("💳 Thanh toán").value("có thể thanh toán bằng cách nào").build()))
                .build();
    }

    // =========================================================================
    // Handler: Order Tracking
    // =========================================================================

    private ChatResponse handleOrderTracking(ChatResponse.ChatResponseBuilder builder,
                                              String userMessage, List<ChatTurn> history, Long userId) {
        // Extract order ID từ message (giữ logic cũ, đã hoạt động tốt)
        String orderIdStr = extractOrderId(userMessage);

        // CASE 1: User đã login, không nhập mã → xem danh sách đơn
        if (orderIdStr == null && userId != null) {
            return handleListOrders(builder, userId);
        }

        // CASE 2: Có mã đơn cụ thể
        if (orderIdStr != null) {
            return handleSingleOrder(builder, orderIdStr, userId, userMessage, history);
        }

        // CASE 3: Chưa login, không có mã đơn
        String response = llmService.generateResponse(
                "User hỏi về đơn hàng nhưng chưa đăng nhập và chưa cung cấp mã đơn. " +
                "Hãy hướng dẫn họ đăng nhập hoặc cung cấp mã đơn (VD: 'đơn 123'). Ngắn gọn.",
                userMessage, history);

        return builder
                .response(response)
                .requiresAuth(true)
                .quickReplies(List.of(
                        QuickReply.builder().label("Đăng nhập").value("tôi muốn đăng nhập").icon("🔐").build()))
                .build();
    }

    private ChatResponse handleListOrders(ChatResponse.ChatResponseBuilder builder, Long userId) {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Order> page = orderRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);

        if (page.isEmpty()) {
            return builder.response("Bạn chưa có đơn hàng nào tại MemeShop. 🛍️\nHãy khám phá sản phẩm và mua sắm nhé!")
                    .quickReplies(List.of(QuickReply.builder().label("🔍 Tìm sản phẩm").value("tôi muốn tìm sản phẩm").build()))
                    .build();
        }

        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        StringBuilder msg = new StringBuilder("📦 **Đơn hàng gần đây của bạn:**\n\n");
        for (Order o : page.getContent()) {
            msg.append(String.format("• Đơn #%d — %s — %s\n",
                    o.getId(), getStatusEmoji(o.getStatus().name()) + " " + o.getStatus(),
                    vndFormat.format(o.getTotalAmount())));
        }
        msg.append("\n💡 Hỏi tôi về 'đơn 123' để xem chi tiết bất kỳ đơn nào!");

        List<Map<String, Object>> ordersData = page.getContent().stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("status", o.getStatus().toString());
            m.put("totalAmount", o.getTotalAmount());
            m.put("createdAt", o.getCreatedAt());
            return m;
        }).collect(Collectors.toList());

        List<QuickReply> replies = page.getContent().stream().limit(3).map(o ->
                QuickReply.builder().label("Xem đơn #" + o.getId()).value("đơn " + o.getId()).icon("📦").build()
        ).collect(Collectors.toList());

        return builder.response(msg.toString())
                .data(Map.of("orders", ordersData))
                .quickReplies(replies)
                .build();
    }

    private ChatResponse handleSingleOrder(ChatResponse.ChatResponseBuilder builder,
                                            String orderIdStr, Long userId,
                                            String userMessage, List<ChatTurn> history) {
        try {
            Long orderId = Long.parseLong(orderIdStr);
            Optional<Order> orderOpt = orderRepository.findById(orderId);

            if (orderOpt.isEmpty()) {
                return builder.response(String.format(
                        "Không tìm thấy đơn hàng #%d. 🔍 Vui lòng kiểm tra lại mã đơn hoặc liên hệ support.", orderId)).build();
            }

            Order order = orderOpt.get();

            // Security check
            if (userId == null) {
                return builder
                        .response("🔒 Vui lòng đăng nhập để xem chi tiết đơn hàng #" + orderId + ".")
                        .requiresAuth(true).build();
            }

            if (!order.getUser().getId().equals(userId)) {
                return builder.response("⚠️ Đơn hàng #" + orderId + " không thuộc về tài khoản của bạn.").build();
            }

            // LLM tổng hợp status message tự nhiên
            String orderInfo = String.format(
                    "Đơn hàng #%d, trạng thái: %s, tổng tiền: %.0f VNĐ, đặt lúc: %s",
                    order.getId(), order.getStatus(), order.getTotalAmount(), order.getCreatedAt());

            String systemPrompt = "Thông báo trạng thái đơn hàng ngắn gọn, thân thiện dựa trên thông tin sau: " + orderInfo;
            String response = llmService.generateResponse(systemPrompt, userMessage, history);

            Map<String, Object> orderData = new LinkedHashMap<>();
            orderData.put("id", order.getId());
            orderData.put("status", order.getStatus());
            orderData.put("paymentStatus", order.getPaymentStatus());
            orderData.put("totalAmount", order.getTotalAmount());
            orderData.put("createdAt", order.getCreatedAt());

            return builder.response(response)
                    .data(Map.of("order", orderData))
                    .quickReplies(List.of(
                            QuickReply.builder().label("Xem chi tiết").value("chi tiết đơn " + orderId).build()))
                    .build();

        } catch (NumberFormatException e) {
            return builder.response("Mã đơn hàng không hợp lệ. Vui lòng nhập số, ví dụ: 'đơn 123' 📝").build();
        }
    }

    // =========================================================================
    // Handler: Other / Fallback
    // =========================================================================

    private ChatResponse handleOther(ChatResponse.ChatResponseBuilder builder,
                                      String userMessage, List<ChatTurn> history) {
        String systemPrompt = """
                User hỏi một câu không rõ ràng hoặc ngoài phạm vi hỗ trợ.
                Hãy:
                1. Thừa nhận nhẹ nhàng rằng bạn không chắc ý họ muốn gì
                2. Gợi ý 2-3 điều bạn CÓ THỂ giúp (sản phẩm, đơn hàng, chính sách)
                3. Hỏi họ muốn làm gì
                Giữ ngắn gọn, không quá 4 câu.
                """;

        String response = llmService.generateResponse(systemPrompt, userMessage, history);

        return builder
                .response(response)
                .quickReplies(Arrays.asList(
                        QuickReply.builder().label("🔍 Tìm sản phẩm").value("tôi muốn tìm sản phẩm").build(),
                        QuickReply.builder().label("📦 Đơn hàng").value("xem đơn hàng của tôi").build(),
                        QuickReply.builder().label("💬 Liên hệ").value("tôi muốn liên hệ support").icon("💬").build()))
                .build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Tìm kiếm sản phẩm nâng cao bằng ProductSpecifications (Tuần 4)
     */
    private List<Product> searchProductsAdvanced(String keyword, String categorySlug, Double minPrice, Double maxPrice, String brand, Double minRating, int limit) {
        log.info("Advanced Search: kw='{}', cat='{}', brand='{}', minPrice={}, maxPrice={}, minRating={}",
                  keyword, categorySlug, brand, minPrice, maxPrice, minRating);
                  
        Pageable pageable = PageRequest.of(0, limit);
        Specification<Product> spec = ProductSpecifications.search(keyword, categorySlug, minPrice, maxPrice, brand, minRating);
        
        Page<Product> page = productRepository.findAll(spec, pageable);
        List<Product> results = new ArrayList<>(page.getContent());

        // Nếu ko tìm được bằng filter nghiêm ngặt, thử nới lỏng từ khóa
        if (results.isEmpty() && keyword != null && !keyword.isBlank()) {
             log.info("No results found, relaxing search constraints...");
             Specification<Product> relaxedSpec = ProductSpecifications.search(keyword, null, null, null, null, null);
             results = new ArrayList<>(productRepository.findAll(relaxedSpec, pageable).getContent());
        }

        // Sort by sold count in memory (for top K constraint) 
        // Trong DB lớn nên sort ngay trong PageRequest, nhưng với limit=5 thì sort List vẫn ok.
        results.sort((a, b) -> Integer.compare(
                b.getSoldCount() != null ? b.getSoldCount() : 0,
                a.getSoldCount() != null ? a.getSoldCount() : 0));

        return results;
    }

    /**
     * Kiểm tra xem câu hỏi hiện tại có phải follow-up về sản phẩm đã bàn không.
     */
    private boolean isProductFollowUp(String userMessage, List<ChatTurn> history) {
        if (history == null || history.isEmpty()) return false;
        String lastIntent = contextService.getLastIntent(
                history.isEmpty() ? "" : ""); // Sẽ dùng sessionId thực trong final version

        String msgLower = userMessage.toLowerCase();
        return msgLower.contains("con nào") || msgLower.contains("cái nào") ||
               msgLower.contains("loại nào") || msgLower.contains("model nào") ||
               msgLower.contains("hơn không") || msgLower.contains("tốt hơn") ||
               (msgLower.contains("nó") && "product".equals(lastIntent));
    }

    /**
     * Trích xuất order ID từ message: "đơn 123", "#456", "order 789"
     */
    private String extractOrderId(String message) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?:đơn|order|#)\\s*(\\d+)");
        java.util.regex.Matcher m = p.matcher(message.toLowerCase());
        return m.find() ? m.group(1) : null;
    }

    private String getStatusEmoji(String status) {
        return switch (status) {
            case "PENDING"          -> "⏳";
            case "CONFIRMED"        -> "✅";
            case "SHIPPED"          -> "🚚";
            case "DELIVERED"        -> "✅";
            case "CANCELED"         -> "❌";
            case "RETURN_REQUESTED" -> "🔄";
            default                  -> "📦";
        };
    }

    private void saveChatMessage(ChatRequest request, ChatResponse response) {
        try {
            chatMessageRepository.save(ChatMessage.builder()
                    .sessionId(request.getSessionId())
                    .message(request.getMessage())
                    .response("")
                    .messageType(ChatMessage.MessageType.USER)
                    .userId(request.getUserId())
                    .build());

            chatMessageRepository.save(ChatMessage.builder()
                    .sessionId(request.getSessionId())
                    .message(request.getMessage())
                    .response(response.getResponse())
                    .messageType(ChatMessage.MessageType.BOT)
                    .intent(response.getIntent())
                    .userId(request.getUserId())
                    .build());
        } catch (Exception e) {
            log.error("Error saving chat message", e);
        }
    }
}
