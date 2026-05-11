package com.example.MyWeb.service;

import com.example.MyWeb.dto.ChatRequest;
import com.example.MyWeb.dto.ChatResponse;
import com.example.MyWeb.dto.ChatTurn;
import com.example.MyWeb.dto.ChatHistoryItemResponse;
import com.example.MyWeb.dto.QuickReply;
import com.example.MyWeb.model.ChatMessage;
import com.example.MyWeb.model.FaqDocument;
import com.example.MyWeb.model.Order;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.repository.ChatMessageRepository;
import com.example.MyWeb.service.ChatHistoryService;
import com.example.MyWeb.repository.ChatbotKnowledgeRepository;
import com.example.MyWeb.repository.OrderRepository;
import com.example.MyWeb.repository.ProductRepository;
import com.example.MyWeb.repository.spec.ProductSpecifications;
import com.example.MyWeb.service.impl.GeminiLlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.MyWeb.dto.ProductSearchConstraints;
import org.springframework.data.domain.Sort;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered Chatbot Service — Tuần 2 Implementation (RAG Integration)
 *
 * Pipeline:
 * 1. Load conversation context (in-memory)
 * 2. LLM classify intent (Gemini gemini-flash-latest)
 * 3. Route theo intent:
 * - product → extract constraints → query DB → LLM diễn đạt
 * - policy → hardcoded + LLM (RAG sẽ thêm ở Tuần 2-3)
 * - order → query DB → format response
 * - greeting → welcome message
 * 4. Update context + save to DB
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatHistoryService chatHistoryService;
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
    // KHÔNG dùng @Transactional ở đây: phương thức này gọi API ngoài (Gemini,
    // 15-30s)
    // lẫn với DB ops. Nếu giữ @Transactional, mọi RuntimeException từ Gemini sẽ
    // mark
    // transaction là rollback-only → gây lỗi "Transaction silently rolled back" ở
    // câu thứ 2 trở đi.
    // Các repository method đã có @Transactional riêng của chúng.
    public ChatResponse processMessage(ChatRequest request) {
        String sessionId = request.getSessionId();
        String userMessage = request.getMessage().trim();

        log.info("[Chatbot] Session={} | User: {}", sessionId.substring(0, 8), userMessage);

        // 1. Load conversation context
        List<ChatTurn> history = contextService.getHistory(sessionId);

        // Nâng cấp: Nếu context rỗng (hôm sau user quay lại hoặc server vừa restart),
        // tiến hành phục hồi lịch sử gần nhất từ Database để AI giữ được ngữ cảnh cũ
        if (history.isEmpty()) {
            List<ChatMessage> dbHistory = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            if (!dbHistory.isEmpty()) {
                contextService.restoreFromDb(sessionId, dbHistory);
                history = contextService.getHistory(sessionId);
                log.info("[Chatbot] Restored {} turns from DB for session {}", history.size(), sessionId);
            }
        }

        // 2. Resolve intent: rule-based safety net + Gemini hỗ trợ
        String intent = resolveIntent(userMessage, history, sessionId);

        log.info("[Chatbot] Intent: '{}'", intent);

        // 3. Update context with user turn
        contextService.addUserTurn(sessionId, userMessage);

        // 4. Generate response based on intent (bọc khỏi exception nếu handler bị lỗi)
        ChatResponse response;
        try {
            response = routeAndRespond(intent, userMessage, history, request);
        } catch (Exception e) {
            log.error("[Chatbot] routeAndRespond failed for intent '{}': {}", intent, e.getMessage(), e);
            response = ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(intent)
                    .response("Xin lỗi, tôi gặp sự cố khi xử lý yêu cầu. Bạn vui lòng thử lại sau nhé! 🙏")
                    .build();
        }

        // 5. Update context with bot response
        contextService.addBotTurn(sessionId, response.getResponse(), intent);

        // 6. Save to DB qua ChatHistoryService (transaction REQUIRES_NEW hoạt động đúng)
        chatHistoryService.saveChatMessage(request, response);

        return response;
    }

    @Override
    public List<ChatHistoryItemResponse> getChatHistory(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        
        return messages.stream().map(msg -> {
            ChatHistoryItemResponse response = new ChatHistoryItemResponse();
            response.setRole(msg.getMessageType() == ChatMessage.MessageType.USER ? "user" : "bot");
            response.setContent(msg.getMessageType() == ChatMessage.MessageType.USER ? msg.getMessage() : msg.getResponse());
            response.setIntent(msg.getIntent());
            response.setSessionId(msg.getSessionId());
            response.setCreatedAt(msg.getCreatedAt());
            
            try {
                if (msg.getResponseData() != null && !msg.getResponseData().isEmpty()) {
                    Map<String, Object> data = objectMapper.readValue(msg.getResponseData(), new TypeReference<Map<String, Object>>() {});
                    response.setData(data);
                }
                if (msg.getQuickReplies() != null && !msg.getQuickReplies().isEmpty()) {
                    List<QuickReply> quickReplies = objectMapper.readValue(msg.getQuickReplies(), new TypeReference<List<QuickReply>>() {});
                    response.setQuickReplies(quickReplies);
                }
            } catch (Exception e) {
                log.error("Failed to parse JSON payload for chat history", e);
            }
            
            return response;
        }).toList();
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
            case "product" -> handleProductSearch(builder, userMessage, history, request.getSessionId());
            case "policy" -> handlePolicyQuestion(builder, userMessage, history);
            case "order" -> handleOrderTracking(builder, userMessage, history, request.getUserId());
            default -> handleOther(builder, userMessage, history);
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
                        QuickReply.builder().label("📦 Đơn hàng của tôi").value("xem đơn hàng của tôi").icon("📦")
                                .build(),
                        QuickReply.builder().label("🚚 Chính sách giao hàng").value("chính sách giao hàng như thế nào")
                                .icon("🚚").build(),
                        QuickReply.builder().label("🔄 Đổi trả hàng").value("chính sách đổi trả hàng").icon("🔄")
                                .build()))
                .build();
    }

    // =========================================================================
    // Handler: Product Search (quan trọng nhất)
    // =========================================================================

    private ChatResponse handleProductSearch(ChatResponse.ChatResponseBuilder builder,
            String userMessage,
            List<ChatTurn> history,
            String sessionId) {

        boolean heuristicFollowUp = isProductFollowUp(userMessage, history, sessionId);

        ProductSearchConstraints constraints = extractConstraintsWithFallback(userMessage, history);

        if (Boolean.TRUE.equals(constraints.getIsFollowUp()) || heuristicFollowUp) {
            constraints = mergeMissingConstraintsFromHistory(constraints, userMessage, history);
        }

        normalizeConstraintsForCatalog(constraints, userMessage);

        log.info("[ProductSearch] constraints: keyword='{}', category='{}', brand='{}', maxPrice={}",
                constraints.getKeyword(), constraints.getCategorySlug(),
                constraints.getBrand(), constraints.getMaxPrice());

        List<Product> products = searchProductsAdvanced(constraints, 5);
        log.info("[ProductSearch] found {} products (exact)", products.size());

        if (products.isEmpty()) {
            List<Product> nearestProducts = searchNearestProducts(constraints, 5);
            log.info("[ProductSearch] found {} products (nearest fallback)", nearestProducts.size());

            if (!nearestProducts.isEmpty()) {
                List<Map<String, Object>> nearestData = nearestProducts.stream().map(p -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("id", p.getId());
                    data.put("name", p.getName());
                    data.put("price", p.getBasePrice());
                    data.put("imageUrl", p.getThumbnailUrl());
                    data.put("slug", p.getSlug());
                    data.put("detailUrl", "/products/" + (p.getSlug() != null ? p.getSlug() : p.getId()));
                    data.put("brand", p.getBrand());
                    data.put("rating", p.getAverageRating());
                    return data;
                }).collect(Collectors.toList());

                Map<String, Object> nearestMeta = new LinkedHashMap<>();
                nearestMeta.put("keyword", constraints.getKeyword());
                nearestMeta.put("category", constraints.getCategorySlug());
                nearestMeta.put("exactMatch", false);

                Map<String, Object> nearestPayload = new LinkedHashMap<>();
                nearestPayload.put("products", nearestData);
                nearestPayload.put("searchMeta", nearestMeta);

                String nearestResponse;
                if (constraints.getMaxPrice() != null) {
                    nearestResponse = String.format(
                            "Mình chưa thấy mẫu %s nào khớp hoàn toàn với mức dưới %,.0fđ. Tuy nhiên, có vài mẫu gần nhất để bạn tham khảo nhé! 😊",
                            hasText(constraints.getKeyword()) ? constraints.getKeyword() : "sản phẩm",
                            constraints.getMaxPrice());
                } else {
                    nearestResponse = "Mình chưa thấy sản phẩm khớp hoàn toàn, nhưng có vài mẫu gần nhất để bạn tham khảo nhé! 😊";
                }

                return builder
                        .response(nearestResponse)
                        .data(nearestPayload)
                        .quickReplies(List.of(
                                QuickReply.builder().label("Xem tất cả kết quả").value("xem tất cả sản phẩm tương tự").build(),
                                QuickReply.builder().label("Tìm loại khác").value("tôi muốn tìm loại sản phẩm khác").build()))
                        .build();
            }

            // Không có nearest cũng không có: hỏi lại ngắn gọn
            return builder
                    .response("Mình chưa tìm thấy sản phẩm phù hợp trong dữ liệu hiện tại. Bạn muốn đổi ngân sách, màu sắc hoặc loại sản phẩm không ạ?")
                    .quickReplies(List.of(
                            QuickReply.builder().label("Tìm lại").value("tôi muốn tìm sản phẩm khác").build()))
                    .build();
        }

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

        String productListText = products.stream()
                .map(p -> String.format("- %s (%s): %.0f VNĐ, đánh giá: %.1f★",
                        p.getName(),
                        p.getBrand() != null ? p.getBrand() : "N/A",
                        p.getBasePrice(),
                        p.getAverageRating() != null ? p.getAverageRating() : 0.0))
                .collect(Collectors.joining("\n"));

        boolean shouldClarify = shouldAskClarification(constraints, heuristicFollowUp, products);

        String clarifyInstruction = shouldClarify
                ? "\nSau khi nêu 2-3 sản phẩm tiêu biểu, kết thúc bằng đúng 1 câu hỏi ngắn để làm rõ thêm nhu cầu."
                : "\nKết thúc bằng gợi ý xem chi tiết hoặc hỏi thêm về nhu cầu.";

        String systemPrompt = String.format("""
                === NHIỆM VỤ ===
                Tư vấn sản phẩm cho khách hàng dựa trên danh sách tìm được từ database.%s

                === DANH SÁCH SẢN PHẨM TÌM ĐƯỢC ===
                %s

                === YÊU CẦU CỦA KHÁCH ===
                Hãy nhắc lại ngắn gọn yêu cầu và tư vấn cụ thể theo nhu cầu đó.
                Nếu user ưu tiên giá rẻ thì nhấn mạnh phương án tiết kiệm hơn.
                Nếu user ưu tiên tốt nhất / đánh giá cao thì nhấn mạnh sản phẩm nổi bật hơn.
                """, clarifyInstruction, productListText);

        String response = llmService.generateResponse(systemPrompt, userMessage, history);

        List<QuickReply> quickReplies = new ArrayList<>();
        if (products.size() == 5) {
            quickReplies.add(QuickReply.builder()
                    .label("Xem thêm kết quả")
                    .value("cho xem thêm sản phẩm tương tự")
                    .build());
        }
        quickReplies.add(QuickReply.builder().label("So sánh").value("so sánh các sản phẩm này").build());
        quickReplies.add(QuickReply.builder().label("Tìm loại khác").value("tôi muốn tìm loại sản phẩm khác").build());

        Map<String, Object> searchMeta = new LinkedHashMap<>();
        if (hasText(constraints.getKeyword())) {
            searchMeta.put("keyword", constraints.getKeyword());
        }
        if (hasText(constraints.getCategorySlug())) {
            searchMeta.put("category", constraints.getCategorySlug());
        }
        if (hasText(constraints.getBrand())) {
            searchMeta.put("brand", constraints.getBrand());
        }
        if (constraints.getMinPrice() != null) {
            searchMeta.put("minPrice", constraints.getMinPrice());
        }
        if (constraints.getMaxPrice() != null) {
            searchMeta.put("maxPrice", constraints.getMaxPrice());
        }
        if (constraints.getMinRating() != null) {
            searchMeta.put("minRating", constraints.getMinRating());
        }
        if (hasText(constraints.getSortBy())) {
            searchMeta.put("sortBy", constraints.getSortBy());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("products", productData);
        payload.put("searchMeta", searchMeta);

        return builder
                .response(response)
                .data(payload)
                .quickReplies(quickReplies)
                .build();
    }

    // =========================================================================
    // Handler: Policy/FAQ (Tuần 2: RAG Integration)
    // =========================================================================

    private ChatResponse handlePolicyQuestion(ChatResponse.ChatResponseBuilder builder,
            String userMessage, List<ChatTurn> history) {

        // 1. RAG Retrieve
        List<FaqDocument> relevantDocs = ragService.retrieveRelevantContext(userMessage, 3);
        log.info("[RAG] retrieved {} FAQ docs for query='{}'", relevantDocs.size(), userMessage);

        // 2. Nếu không tìm thấy FAQ: trả hardcoded, không để LLM bịa chính sách
        if (relevantDocs.isEmpty()) {
            return builder
                    .response("Mình chưa tìm thấy tài liệu chính sách phù hợp trong kho FAQ hiện tại. " +
                            "Bạn có thể hỏi cụ thể hơn về: thanh toán, đổi trả, giao hàng, bảo hành hoặc voucher nhé 😊")
                    .quickReplies(Arrays.asList(
                            QuickReply.builder().label("💳 Hình thức thanh toán").value("các hình thức thanh toán hiện có").build(),
                            QuickReply.builder().label("🔄 Chính sách đổi trả").value("chính sách đổi trả hàng").build(),
                            QuickReply.builder().label("🚚 Giao hàng").value("chính sách giao hàng").build(),
                            QuickReply.builder().label("🎁 Voucher").value("chính sách voucher").build()))
                    .build();
        }

        // 3. Có FAQ → dùng RAG context để LLM tổng hợp
        String policyContext = ragService.buildContextString(relevantDocs);

        String systemPrompt = String.format("""
                === NGỮ CẢNH CHÍNH SÁCH TỪ FAQ/RAG ===
                %s

                === HƯỚNG DẪN ===
                Bạn là trợ lý AI của MemeShop.
                Chỉ trả lời dựa trên ngữ cảnh chính sách ở trên.
                Nếu có thông tin, trả lời trực tiếp, không hỏi vòng vo.
                Nếu user hỏi về thanh toán, nêu rõ các hình thức thanh toán và quy trình nếu context có.
                Nếu user hỏi về đổi trả/trả hàng, nêu điều kiện cụ thể nếu context có.
                Trả lời ngắn gọn, dùng bullet points, thân thiện.
                """, policyContext);

        String response = llmService.generateResponse(systemPrompt, userMessage, history);

        return builder
                .response(response)
                .quickReplies(Arrays.asList(
                        QuickReply.builder().label("💳 Thanh toán").value("các hình thức thanh toán hiện có").build(),
                        QuickReply.builder().label("🔄 Đổi trả").value("chính sách đổi trả hàng").build(),
                        QuickReply.builder().label("🚚 Giao hàng").value("chính sách giao hàng").build()))
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
                    .quickReplies(List
                            .of(QuickReply.builder().label("🔍 Tìm sản phẩm").value("tôi muốn tìm sản phẩm").build()))
                    .build();
        }

        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        StringBuilder ordersInfo = new StringBuilder();
        for (Order o : page.getContent()) {
            ordersInfo.append(String.format("• Đơn #%d — %s — %s\n",
                    o.getId(), getStatusEmoji(o.getStatus().name()) + " " + o.getStatus(),
                    vndFormat.format(o.getTotalAmount())));
        }

        String systemPrompt = String.format("""
                === DANH SÁCH ĐƠN HÀNG GẦN ĐÂY ===
                %s
                
                === NHIỆM VỤ ===
                Bạn là trợ lý AI của MemeShop. Hãy thông báo danh sách đơn hàng này cho khách hàng một cách tự nhiên, thân thiện.
                Có thể tóm tắt nhanh tình trạng các đơn (ví dụ: 'Bạn có 1 đơn đang giao và 1 đơn đã hoàn thành').
                Kết thúc bằng lời nhắc: Khách có thể nhập mã đơn (ví dụ: 'đơn %d') để xem chi tiết nhé.
                Không bịa thêm thông tin ngoài danh sách trên.
                """, ordersInfo.toString(), page.getContent().get(0).getId());

        String response = llmService.generateResponse(systemPrompt, "xem đơn hàng của tôi", List.of());

        List<Map<String, Object>> ordersData = page.getContent().stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("status", o.getStatus().toString());
            m.put("totalAmount", o.getTotalAmount());
            m.put("createdAt", o.getCreatedAt());
            return m;
        }).collect(Collectors.toList());

        List<QuickReply> replies = page.getContent().stream().limit(3).map(
                o -> QuickReply.builder().label("Xem đơn #" + o.getId()).value("đơn " + o.getId()).icon("📦").build())
                .collect(Collectors.toList());

        return builder.response(response)
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
                        "Không tìm thấy đơn hàng #%d. 🔍 Vui lòng kiểm tra lại mã đơn hoặc liên hệ support.", orderId))
                        .build();
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

            String formattedStatus = switch (order.getStatus()) {
                case PENDING -> "Chờ xác nhận";
                case CONFIRMED -> "Đã xác nhận";
                case SHIPPED -> "Đang giao";
                case DELIVERED -> "Đã giao hàng";
                case CANCELED -> "Đã hủy";
                case RETURN_REQUESTED -> "Yêu cầu trả hàng";
                case RETURNED -> "Đã trả hàng";
                case REFUNDED -> "Đã hoàn tiền";
                default -> order.getStatus().toString();
            };
            String formattedPrice = new java.text.DecimalFormat("#,###").format(order.getTotalAmount()) + " đ";
            
            String itemsText = order.getItems().stream()
                    .map(item -> item.getQuantity() + "x " + item.getProductName())
                    .collect(Collectors.joining(", "));
                    
            String noteText = order.getNote() != null && !order.getNote().trim().isEmpty() 
                    ? order.getNote() : "Không có";
            
            String systemPrompt = String.format("""
                    === THÔNG TIN ĐƠN HÀNG ===
                    Mã đơn: #%d
                    Trạng thái: %s
                    Thanh toán: %s
                    Tổng tiền: %s
                    Sản phẩm: %s
                    Ghi chú/Lý do: %s
                    Ngày cập nhật: %s
                    
                    === NHIỆM VỤ ===
                    Bạn là trợ lý AI của MemeShop. Hãy thông báo tình trạng đơn hàng này cho khách hàng một cách tự nhiên, thân thiện và chuyên nghiệp.
                    Chỉ dựa vào thông tin được cung cấp ở trên. KHÔNG tự bịa thêm sản phẩm hay thông tin khác.
                    Giữ câu trả lời ngắn gọn (2-3 câu) và luôn thân thiện. Hãy nhắc đến sản phẩm trong đơn để khách nhớ.
                    Nếu có ghi chú (đặc biệt là lý do hủy/trả hàng), hãy khéo léo thông báo cho khách.
                    """, order.getId(), formattedStatus, order.getPaymentStatus(), formattedPrice, itemsText, noteText, order.getUpdatedAt());

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
        // Hardcoded — không gọi LLM để tránh hallucination về catalog
        return builder
                .response("Mình chưa xác định rõ yêu cầu của bạn. " +
                        "Hiện mình có thể hỗ trợ: tìm sản phẩm, tư vấn sản phẩm, tra cứu đơn hàng và giải đáp chính sách mua hàng/đổi trả/thanh toán. " +
                        "Bạn muốn mình hỗ trợ theo hướng nào ạ? 😊")
                .quickReplies(Arrays.asList(
                        QuickReply.builder().label("🔍 Tìm sản phẩm").value("tôi muốn tìm sản phẩm").build(),
                        QuickReply.builder().label("💳 Chính sách thanh toán").value("các hình thức thanh toán hiện có").build(),
                        QuickReply.builder().label("🔄 Chính sách đổi trả").value("chính sách đổi trả hàng").build(),
                        QuickReply.builder().label("📦 Đơn hàng").value("xem đơn hàng của tôi").build()))
                .build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Tìm kiếm sản phẩm nâng cao bằng ProductSpecifications (Tuần 4)
     */
    private List<Product> searchProductsAdvanced(ProductSearchConstraints c, int limit) {
        Sort sort = buildSort(c.getSortBy());

        Pageable pageable = PageRequest.of(0, limit, sort);

        Specification<Product> spec = ProductSpecifications.search(
                c.getKeyword(),
                c.getCategorySlug(),
                c.getMinPrice(),
                c.getMaxPrice(),
                c.getBrand(),
                c.getMinRating());

        List<Product> results = new ArrayList<>(productRepository.findAll(spec, pageable).getContent());

        return results;
    }

    /**
     * Kiểm tra xem câu hỏi hiện tại có phải follow-up về sản phẩm đã bàn không.
     * Dùng sessionId thực để lấy lastIntent từ ConversationContextService.
     */
    private boolean isProductFollowUp(String userMessage, List<ChatTurn> history, String sessionId) {
        if (history == null || history.isEmpty())
            return false;

        // Lấy intent gần nhất của bot từ in-memory context (đã fix: dùng sessionId
        // thực)
        String lastIntent = contextService.getLastIntent(sessionId);

        String msgLower = userMessage.toLowerCase();
        return msgLower.contains("con nào") || msgLower.contains("cái nào")
                || msgLower.contains("loại nào") || msgLower.contains("model nào")
                || msgLower.contains("hơn không") || msgLower.contains("tốt hơn")
                || msgLower.contains("so sánh")
                || msgLower.contains("cái đó") || msgLower.contains("sản phẩm đó")
                || msgLower.contains("loại khác") || msgLower.contains("sản phẩm khác")
                || msgLower.contains("mẫu khác")
                || (msgLower.contains("nó") && "product".equals(lastIntent))
                || (msgLower.length() < 20 && "product".equals(lastIntent)); // Câu rất ngắn trong ctx product
    }

    /**
     * Trích xuất order ID từ message: "đơn 123", "#456", "order 789"
     */
    private String extractOrderId(String message) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?:đơn|order|mã|#)\\s*(\\d+)");
        java.util.regex.Matcher m = p.matcher(message.toLowerCase());
        return m.find() ? m.group(1) : null;
    }

    private String getStatusEmoji(String status) {
        return switch (status) {
            case "PENDING" -> "⏳";
            case "CONFIRMED" -> "✅";
            case "SHIPPED" -> "🚚";
            case "DELIVERED" -> "✅";
            case "CANCELED" -> "❌";
            case "RETURN_REQUESTED" -> "🔄";
            default -> "📦";
        };
    }

    // saveChatMessage đã được chuyển sang ChatHistoryService để
    // @Transactional(REQUIRES_NEW) hoạt động đúng qua Spring AOP proxy.
    // Self-invocation trong cùng class không đi qua proxy → annotation bị bỏ qua.

    private ProductSearchConstraints extractConstraintsWithFallback(String userMessage, List<ChatTurn> history) {
        ProductSearchConstraints c = ProductSearchConstraints.builder()
                .isFollowUp(false)
                .build();

        try {
            String constraintsJson = llmService.extractProductConstraints(userMessage, history);
            JsonNode node = objectMapper.readTree(constraintsJson);

            c.setKeyword(textOrNull(node, "keyword"));
            c.setCategorySlug(textOrNull(node, "categorySlug"));
            c.setBrand(textOrNull(node, "brand"));
            c.setSortBy(textOrNull(node, "sortBy"));
            c.setIsFollowUp(node.path("isFollowUp").asBoolean(false));

            if (node.hasNonNull("minPrice") && node.get("minPrice").isNumber()) {
                c.setMinPrice(node.get("minPrice").asDouble());
            }
            if (node.hasNonNull("maxPrice") && node.get("maxPrice").isNumber()) {
                c.setMaxPrice(node.get("maxPrice").asDouble());
            }
            if (node.hasNonNull("minRating") && node.get("minRating").isNumber()) {
                c.setMinRating(node.get("minRating").asDouble());
            }
        } catch (Exception e) {
            log.warn("[ProductSearch] Cannot parse LLM constraints, fallback to rule-based extraction", e);
        }

        // Rule-based fallback
        if (!hasText(c.getCategorySlug()))
            c.setCategorySlug(inferCategoryFromText(userMessage));
        if (!hasText(c.getBrand()))
            c.setBrand(inferBrandFromText(userMessage));
        if (!hasText(c.getSortBy()))
            c.setSortBy(inferSortBy(userMessage));
        if (!hasText(c.getKeyword()))
            c.setKeyword(inferKeywordFromText(userMessage, c.getCategorySlug()));

        return c;
    }

    private ProductSearchConstraints mergeMissingConstraintsFromHistory(ProductSearchConstraints current,
            String userMessage,
            List<ChatTurn> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatTurn turn = history.get(i);
            if (!"user".equals(turn.getRole()))
                continue;
            if (turn.getContent().equalsIgnoreCase(userMessage))
                continue;

            ProductSearchConstraints prev = extractConstraintsWithFallback(turn.getContent(), List.of());

            if (!hasText(current.getKeyword()))
                current.setKeyword(prev.getKeyword());
            if (!hasText(current.getCategorySlug()))
                current.setCategorySlug(prev.getCategorySlug());
            if (!hasText(current.getBrand()))
                current.setBrand(prev.getBrand());
            if (current.getMinPrice() == null)
                current.setMinPrice(prev.getMinPrice());
            if (current.getMaxPrice() == null)
                current.setMaxPrice(prev.getMaxPrice());
            if (current.getMinRating() == null)
                current.setMinRating(prev.getMinRating());
            if (!hasText(current.getSortBy()))
                current.setSortBy(prev.getSortBy());

            if (hasText(current.getCategorySlug()) || hasText(current.getBrand()) || hasText(current.getKeyword())) {
                break;
            }
        }
        return current;
    }

    private void normalizeConstraintsForCatalog(ProductSearchConstraints c, String userMessage) {
        // Normalize váy/đầm/dress trước để đảm bảo category được set
        if (containsAny(userMessage, "váy", "đầm", "dress")) {
            c.setCategorySlug("fashion");
            if (!hasText(c.getKeyword())) {
                c.setKeyword("váy");
            } else {
                String kw = c.getKeyword().toLowerCase(Locale.ROOT);
                // Nếu keyword quá nhiễu (LLM bịa tính từ), normalize về "váy"
                if (containsAny(kw, "đẹp", "xinh", "giá rẻ", "rẻ", "phải chăng")) {
                    c.setKeyword("váy");
                }
            }
        }

        // Generic keyword -> category only
        if (hasText(c.getKeyword())) {
            String kw = c.getKeyword().trim().toLowerCase(Locale.ROOT);

            if (kw.contains("điện thoại") || kw.contains("smartphone") || kw.contains("phone") || kw.contains("mobile")) {
                kw = kw.replace("điện thoại", "").replace("smartphone", "").replace("phone", "").replace("mobile", "").trim();
                if (!hasText(c.getCategorySlug()))
                    c.setCategorySlug("electronics");
            }
            if (kw.contains("quần áo") || kw.contains("thời trang") || kw.contains("fashion")) {
                kw = kw.replace("quần áo", "").replace("thời trang", "").replace("fashion", "").trim();
                if (!hasText(c.getCategorySlug()))
                    c.setCategorySlug("fashion");
            }
            if (kw.contains("mỹ phẩm") || kw.contains("skincare") || kw.contains("beauty")) {
                kw = kw.replace("mỹ phẩm", "").replace("skincare", "").replace("beauty", "").trim();
                if (!hasText(c.getCategorySlug()))
                    c.setCategorySlug("beauty");
            }
            if (kw.contains("nội thất") || kw.contains("home") || kw.contains("home living")) {
                kw = kw.replace("nội thất", "").replace("home living", "").replace("home", "").trim();
                if (!hasText(c.getCategorySlug()))
                    c.setCategorySlug("home-living");
            }

            c.setKeyword(kw.isEmpty() ? null : kw);
        }

        // Safety guard: phục hồi keyword cụ thể từ user message nếu bị null sau normalize.
        // Quan trọng: tránh chỉ còn categorySlug → query trả toàn bộ category.
        // Thứ tự: từ cụ thể nhất → chung nhất để tránh nhầm lẫn.
        if (!hasText(c.getKeyword())) {
            // Fashion
            if (containsAny(userMessage, "hoodie", "áo hoodie")) {
                c.setKeyword("hoodie"); c.setCategorySlug("fashion");
            } else if (containsAny(userMessage, "sơ mi", "oxford")) {
                c.setKeyword("sơ mi"); c.setCategorySlug("fashion");
            } else if (containsAny(userMessage, "áo thun", "cotton")) {
                c.setKeyword("áo thun"); c.setCategorySlug("fashion");
            } else if (containsAny(userMessage, "váy midi", "midi")) {
                c.setKeyword("váy midi"); c.setCategorySlug("fashion");
            } else if (containsAny(userMessage, "váy công sở")) {
                c.setKeyword("váy công sở"); c.setCategorySlug("fashion");
            } else if (containsAny(userMessage, "váy", "đầm", "dress")) {
                c.setKeyword("váy"); c.setCategorySlug("fashion");
            } else if (containsAny(userMessage, "sneaker", "giày sneaker")) {
                c.setKeyword("sneaker"); c.setCategorySlug("fashion");
            } else if (containsAny(userMessage, "vans")) {
                c.setKeyword("vans"); c.setCategorySlug("fashion");
            } else if (containsAny(userMessage, "giày")) {
                c.setKeyword("giày"); c.setCategorySlug("fashion");
            // Home & Living
            } else if (containsAny(userMessage, "công thái học", "ergonomic")) {
                c.setKeyword("công thái học"); c.setCategorySlug("home-living");
            } else if (containsAny(userMessage, "ghế gaming")) {
                c.setKeyword("gaming"); c.setCategorySlug("home-living");
            } else if (containsAny(userMessage, "ghế ăn")) {
                c.setKeyword("ghế ăn"); c.setCategorySlug("home-living");
            } else if (containsAny(userMessage, "ghế")) {
                c.setKeyword("ghế"); c.setCategorySlug("home-living");
            } else if (containsAny(userMessage, "bàn làm việc")) {
                c.setKeyword("bàn làm việc"); c.setCategorySlug("home-living");
            } else if (containsAny(userMessage, "bàn học")) {
                c.setKeyword("bàn học"); c.setCategorySlug("home-living");
            } else if (containsAny(userMessage, "bàn sofa")) {
                c.setKeyword("bàn sofa"); c.setCategorySlug("home-living");
            } else if (containsAny(userMessage, "bàn")) {
                c.setKeyword("bàn"); c.setCategorySlug("home-living");
            // Beauty
            } else if (containsAny(userMessage, "kem mắt", "caffeine")) {
                c.setKeyword("kem mắt"); c.setCategorySlug("beauty");
            } else if (containsAny(userMessage, "chống lão hóa", "peptide")) {
                c.setKeyword("kem chống lão hóa"); c.setCategorySlug("beauty");
            } else if (containsAny(userMessage, "dưỡng ẩm", "hyaluronic")) {
                c.setKeyword("kem dưỡng ẩm"); c.setCategorySlug("beauty");
            } else if (containsAny(userMessage, "mặt nạ ngủ", "sleeping")) {
                c.setKeyword("mặt nạ ngủ"); c.setCategorySlug("beauty");
            } else if (containsAny(userMessage, "đất sét")) {
                c.setKeyword("đất sét"); c.setCategorySlug("beauty");
            } else if (containsAny(userMessage, "mặt nạ giấy", "sheet mask")) {
                c.setKeyword("mặt nạ giấy"); c.setCategorySlug("beauty");
            } else if (containsAny(userMessage, "mặt nạ")) {
                c.setKeyword("mặt nạ"); c.setCategorySlug("beauty");
            // Electronics
            } else if (containsAny(userMessage, "amoled", "đồng hồ thông minh")) {
                c.setKeyword("đồng hồ thông minh"); c.setCategorySlug("electronics");
            } else if (containsAny(userMessage, "gps", "đồng hồ thể thao")) {
                c.setKeyword("đồng hồ thể thao"); c.setCategorySlug("electronics");
            } else if (containsAny(userMessage, "đồng hồ", "smartwatch", "watch")) {
                c.setKeyword("đồng hồ"); c.setCategorySlug("electronics");
            }
        }
    }

    private boolean shouldAskClarification(ProductSearchConstraints c,
            boolean heuristicFollowUp,
            List<Product> products) {
        boolean broad = !hasText(c.getKeyword())
                && hasText(c.getCategorySlug())
                && !hasText(c.getBrand())
                && c.getMinPrice() == null
                && c.getMaxPrice() == null
                && c.getMinRating() == null;

        return !Boolean.TRUE.equals(c.getIsFollowUp()) && !heuristicFollowUp && broad && products.size() >= 3;
    }

    private Sort buildSort(String sortBy) {
        if (!hasText(sortBy)) {
            return Sort.by(
                    Sort.Order.desc("averageRating"),
                    Sort.Order.desc("soldCount"),
                    Sort.Order.desc("createdAt"));
        }

        return switch (sortBy) {
            case "priceAsc" -> Sort.by(Sort.Order.asc("basePrice"), Sort.Order.desc("averageRating"));
            case "priceDesc" -> Sort.by(Sort.Order.desc("basePrice"), Sort.Order.desc("averageRating"));
            case "bestSelling" -> Sort.by(Sort.Order.desc("soldCount"), Sort.Order.desc("averageRating"));
            case "topRated" ->
                Sort.by(Sort.Order.desc("averageRating"), Sort.Order.desc("reviewCount"), Sort.Order.desc("soldCount"));
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"));
            default ->
                Sort.by(Sort.Order.desc("averageRating"), Sort.Order.desc("soldCount"), Sort.Order.desc("createdAt"));
        };
    }

    private String inferCategoryFromText(String text) {
        String msg = safeLower(text);

        // Electronics: điện thoại, đồng hồ thông minh/GPS
        if (containsAny(msg, "iphone", "samsung", "galaxy", "apple",
                "điện thoại", "smartphone", "đồng hồ", "watch", "amoled", "gps")) {
            return "electronics";
        }
        // Fashion: áo (thun/hoodie/sơ mi), váy, giày sneaker
        if (containsAny(msg, "áo thun", "áo hoodie", "hoodie", "áo sơ mi", "sơ mi",
                "váy", "đầm", "giày", "sneaker", "vans", "áo")) {
            return "fashion";
        }
        // Home & Living: ghế (công thái học/gaming/ăn), bàn (làm việc/học/sofa)
        if (containsAny(msg, "ghế", "bàn làm việc", "bàn học", "bàn sofa",
                "nội thất", "bàn", "bàn gỗ")) {
            return "home-living";
        }
        // Beauty: kem (dưỡng ẩm/chống lão hóa/mắt), mặt nạ (đất sét/giấy/ngủ)
        if (containsAny(msg, "kem dưỡng", "kem chống", "kem mắt", "dưỡng ẩm",
                "mặt nạ", "đất sét", "hyaluronic", "peptide", "caffeine",
                "skincare", "mỹ phẩm", "serum")) {
            return "beauty";
        }
        return null;
    }

    private String inferBrandFromText(String text) {
        String msg = safeLower(text);
        if (containsAny(msg, "samsung", "galaxy")) return "samsung";
        if (containsAny(msg, "apple", "iphone")) return "apple";
        if (containsAny(msg, "vans")) return "vans";
        return null;
    }

    private String inferKeywordFromText(String text, String categorySlug) {
        String msg = safeLower(text);

        if ("fashion".equals(categorySlug)) {
            // Thứ tự ưu tiên: cụ thể → chung
            if (msg.contains("hoodie") || msg.contains("áo hoodie")) return "hoodie";
            if (msg.contains("sơ mi") || msg.contains("oxford"))     return "sơ mi";
            if (msg.contains("áo thun") || msg.contains("cotton"))   return "áo thun";
            if (msg.contains("váy midi") || msg.contains("midi"))     return "váy midi";
            if (msg.contains("váy công sở"))                         return "váy công sở";
            if (msg.contains("váy") || msg.contains("đầm"))           return "váy";
            if (msg.contains("sneaker") || msg.contains("giày sneaker")) return "sneaker";
            if (msg.contains("vans"))                                return "vans";
            if (msg.contains("giày"))                                return "giày";
            if (msg.contains("áo"))                                  return "áo";
        }

        if ("home-living".equals(categorySlug)) {
            if (msg.contains("công thái học") || msg.contains("ergonomic")) return "công thái học";
            if (msg.contains("ghế gaming") || msg.contains("gaming"))       return "gaming";
            if (msg.contains("ghế ăn"))                                     return "ghế ăn";
            if (msg.contains("ghế"))                                        return "ghế";
            if (msg.contains("bàn làm việc"))                               return "bàn làm việc";
            if (msg.contains("bàn học"))                                    return "bàn học";
            if (msg.contains("bàn sofa"))                                   return "bàn sofa";
            if (msg.contains("bàn"))                                        return "bàn";
        }

        if ("beauty".equals(categorySlug)) {
            if (msg.contains("kem mắt") || msg.contains("caffeine"))               return "kem mắt";
            if (msg.contains("chống lão hóa") || msg.contains("peptide"))          return "kem chống lão hóa";
            if (msg.contains("dưỡng ẩm") || msg.contains("hyaluronic"))            return "kem dưỡng ẩm";
            if (msg.contains("mặt nạ ngủ") || msg.contains("sleeping mask"))        return "mặt nạ ngủ";
            if (msg.contains("mặt nạ đất sét") || msg.contains("đất sét"))         return "mặt nạ đất sét";
            if (msg.contains("mặt nạ giấy") || msg.contains("sheet mask"))         return "mặt nạ giấy";
            if (msg.contains("mặt nạ"))                                            return "mặt nạ";
            if (msg.contains("kem"))                                               return "kem";
        }

        if ("electronics".equals(categorySlug)) {
            if (msg.contains("iphone 15")) return "iphone 15";
            if (msg.contains("iphone 14")) return "iphone 14";
            if (msg.contains("iphone"))   return "iphone";
            if (msg.contains("s24 ultra") || msg.contains("galaxy s24")) return "galaxy s24";
            if (msg.contains("s23") || msg.contains("galaxy s23"))       return "galaxy s23";
            if (msg.contains("galaxy"))   return "galaxy";
            if (msg.contains("amoled") || msg.contains("đồng hồ thông minh")) return "đồng hồ thông minh";
            if (msg.contains("gps") || msg.contains("đồng hồ thể thao"))      return "đồng hồ thể thao";
            if (msg.contains("đồng hồ"))  return "đồng hồ";
            return null; // "điện thoại" generic → để null, chỉ dùng category
        }

        return null;
    }

    private String inferSortBy(String text) {
        String msg = safeLower(text);

        if (containsAny(msg, "đánh giá cao", "tốt nhất", "5 sao", "nổi bật"))
            return "topRated";
        if (containsAny(msg, "bán chạy", "phổ biến", "nhiều người mua"))
            return "bestSelling";
        if (containsAny(msg, "rẻ nhất", "giá thấp nhất"))
            return "priceAsc";
        if (containsAny(msg, "cao cấp", "đắt hơn"))
            return "priceDesc";
        if (containsAny(msg, "mới nhất", "mẫu mới"))
            return "newest";

        return null;
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull())
            return null;
        String value = node.get(field).asText(null);
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isBlank();
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... keywords) {
        String normalized = safeLower(text);
        for (String kw : keywords) {
            if (normalized.contains(kw.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // Intent Resolution (Rule-based safety net + Gemini support)
    // =========================================================================

    /**
     * Resolve intent: dùng rule-based trước (chắc chắn), Gemini chỉ là fallback.
     * Không để Gemini là single point of failure.
     */
    private String resolveIntent(String userMessage, List<ChatTurn> history, String sessionId) {
        String ruleIntent = detectIntentByRules(userMessage, history, sessionId);

        // Nếu rule đã chắc thì dùng luôn, không cần hỏi Gemini
        if (!"other".equals(ruleIntent)) {
            log.info("[Chatbot] Rule-based intent: '{}'", ruleIntent);
            return ruleIntent;
        }

        // Rule chưa chắc → hỏi Gemini
        try {
            String llmIntent = llmService.classifyIntent(userMessage, history);
            log.info("[Chatbot] Gemini intent: '{}' for: {}", llmIntent,
                    userMessage.substring(0, Math.min(50, userMessage.length())));

            if (List.of("product", "policy", "order", "greeting", "other").contains(llmIntent)) {
                if ("other".equals(llmIntent) && "product".equals(contextService.getLastIntent(sessionId))
                        && isProductFollowUp(userMessage, history, sessionId)) {
                    log.info("[Chatbot] Override Gemini 'other' to 'product' due to follow-up heuristic.");
                    return "product";
                }
                return llmIntent;
            }

            log.warn("[Chatbot] Gemini returned unexpected intent '{}', fallback to 'other'", llmIntent);
            return "other";
        } catch (Exception e) {
            log.error("[Chatbot] LLM classify failed, rule fallback: {}", ruleIntent, e);
            return ruleIntent; // ruleIntent = "other" lúc này
        }
    }

    /**
     * Rule-based intent detection.
     * Thứ tự: order > policy > product > greeting > follow-up > other
     * Policy check TRƯỚC product để tránh "chính sách mua hàng" bị nhầm thành product.
     */
    private String detectIntentByRules(String message, List<ChatTurn> history, String sessionId) {
        String msg = safeLower(message);

        // 1. Order
        if (extractOrderId(msg) != null || containsAny(msg,
                "đơn hàng", "đơn ", "order", "mã đơn", "trạng thái đơn",
                "kiểm tra đơn", "theo dõi đơn", "đơn của tôi")) {
            return "order";
        }

        // 2. Policy / FAQ / RAG — TRƯỚC product để "chính sách mua hàng" không bị nhầm
        if (containsAny(msg,
                "chính sách", "mua hàng", "trả hàng", "đổi trả", "hoàn trả",
                "bảo hành", "giao hàng", "phí ship", "vận chuyển",
                "thanh toán", "hình thức thanh toán", "quy trình thanh toán",
                "cod", "vnpay", "momo", "hoàn tiền", "voucher", "khuyến mãi")) {
            return "policy";
        }

        // 3. Product — dùng product noun cụ thể từ catalog thực tế
        if (containsAny(msg,
                // Fashion
                "váy", "đầm", "dress", "áo thun", "áo hoodie", "hoodie",
                "áo sơ mi", "sơ mi", "oxford", "giày", "sneaker", "vans",
                // Electronics
                "điện thoại", "iphone", "samsung", "galaxy", "đồng hồ", "amoled",
                // Home & Living
                "ghế", "bàn làm việc", "bàn học", "bàn sofa",
                // Beauty
                "kem dưỡng", "kem chống", "kem mắt", "mặt nạ", "dưỡng ẩm",
                "hyaluronic", "peptide", "skincare", "mỹ phẩm")) {
            return "product";
        }

        // Bao gồm cả "áo" đơn lẻ nhưng KHÔNG bao gồm "áo" trong câu chính sách
        if (containsAny(msg, "áo") && !containsAny(msg, "chính sách", "thanh toán", "đổi trả")) {
            return "product";
        }
        // "bàn" đơn lẻ → home-living
        if (containsAny(msg, "bàn") && !containsAny(msg, "chính sách", "thanh toán")) {
            return "product";
        }

        if (containsAny(msg, "tìm sản phẩm", "mua sản phẩm", "tư vấn sản phẩm", "gợi ý sản phẩm")) {
            return "product";
        }

        // 4. Greeting / Contact
        if (containsAny(msg, "xin chào", "chào", "hello", "hi", "liên hệ", "hotline", "support")) {
            return "greeting";
        }

        // 5. Follow-up theo context session
        String lastIntent = contextService.getLastIntent(sessionId);

        if ("product".equals(lastIntent)
                && containsAny(msg, "cái nào", "con nào", "loại nào", "rẻ hơn", "tốt hơn",
                        "so sánh", "mẫu nào", "đúng rồi", "tìm cho mình", "loại khác", "sản phẩm khác", "mẫu khác")) {
            return "product";
        }

        if ("policy".equals(lastIntent)
                && containsAny(msg, "rõ hơn", "chi tiết", "quy trình", "hình thức",
                        "khâu thanh toán", "trả hàng")) {
            return "policy";
        }

        return "other";
    }

    /**
     * Tìm sản phẩm gần nhất khi exact search trả rỗng.
     * Bỏ filter giá/rating, giữ keyword/category/brand.
     */
    private List<Product> searchNearestProducts(ProductSearchConstraints c, int limit) {
        Pageable pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Order.asc("basePrice"), Sort.Order.desc("averageRating")));

        // Bước 1 relax: bỏ giá/rating, giữ keyword + category + brand
        Specification<Product> spec = ProductSpecifications.search(
                c.getKeyword(),
                c.getCategorySlug(),
                null, // bỏ minPrice
                null, // bỏ maxPrice
                c.getBrand(),
                null  // bỏ minRating
        );

        List<Product> results = new ArrayList<>(productRepository.findAll(spec, pageable).getContent());

        // Bước 2 relax: bỏ brand (vẫn giữ keyword để kết quả chính xác)
        if (results.isEmpty() && hasText(c.getBrand())) {
            Specification<Product> noBrand = ProductSpecifications.search(
                    c.getKeyword(),
                    c.getCategorySlug(),
                    null,
                    null,
                    null,  // bỏ brand
                    null
            );
            results = new ArrayList<>(productRepository.findAll(noBrand, pageable).getContent());
        }

        // Bước 3 relax: chỉ bỏ keyword nếu keyword là null (category-level search)
        // KHÔNG bỏ keyword khi keyword có giá trị cụ thể (váy, giày, ghế...)
        // vì sẽ trả về toàn bộ category → thừa sản phẩm không liên quan
        if (results.isEmpty() && !hasText(c.getKeyword()) && hasText(c.getCategorySlug())) {
            Specification<Product> categoryOnly = ProductSpecifications.search(
                    null,
                    c.getCategorySlug(),
                    null,
                    null,
                    null,
                    null
            );
            results = new ArrayList<>(productRepository.findAll(categoryOnly, pageable).getContent());
        }

        return results;
    }
}
