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

        // 6. Save to DB trong transaction riêng biệt
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

        List<Product> products = searchProductsAdvanced(constraints, 5);

        if (products.isEmpty()) {
            String clarifyResponse = llmService.generateResponse(
                    """
                            Không tìm thấy sản phẩm phù hợp trong DB.
                            Hãy xin lỗi ngắn gọn, nêu rằng hiện chưa thấy sản phẩm khớp hoàn toàn,
                            rồi hỏi user 1 câu ngắn để làm rõ hơn về ngân sách, thương hiệu hoặc loại sản phẩm.
                            """,
                    userMessage,
                    history);

            return builder.response(clarifyResponse)
                    .quickReplies(List.of(
                            QuickReply.builder().label("Xem tất cả sản phẩm").value("xem tất cả sản phẩm").build(),
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
                    .quickReplies(List
                            .of(QuickReply.builder().label("🔍 Tìm sản phẩm").value("tôi muốn tìm sản phẩm").build()))
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

        List<QuickReply> replies = page.getContent().stream().limit(3).map(
                o -> QuickReply.builder().label("Xem đơn #" + o.getId()).value("đơn " + o.getId()).icon("📦").build())
                .collect(Collectors.toList());

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

            // LLM tổng hợp status message tự nhiên
            String orderInfo = String.format(
                    "Đơn hàng #%d, trạng thái: %s, tổng tiền: %.0f VNĐ, đặt lúc: %s",
                    order.getId(), order.getStatus(), order.getTotalAmount(), order.getCreatedAt());

            String systemPrompt = "Thông báo trạng thái đơn hàng ngắn gọn, thân thiện dựa trên thông tin sau: "
                    + orderInfo;
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

        // Relax 1: bỏ keyword nhưng giữ category/brand/price/rating
        if (results.isEmpty() && hasText(c.getKeyword())) {
            Specification<Product> relaxedKeyword = ProductSpecifications.search(
                    null,
                    c.getCategorySlug(),
                    c.getMinPrice(),
                    c.getMaxPrice(),
                    c.getBrand(),
                    c.getMinRating());
            results = new ArrayList<>(productRepository.findAll(relaxedKeyword, pageable).getContent());
        }

        // Relax 2: nếu user yêu cầu "đánh giá cao" quá gắt thì thử bỏ minRating
        if (results.isEmpty() && c.getMinRating() != null) {
            Specification<Product> relaxedRating = ProductSpecifications.search(
                    c.getKeyword(),
                    c.getCategorySlug(),
                    c.getMinPrice(),
                    c.getMaxPrice(),
                    c.getBrand(),
                    null);
            results = new ArrayList<>(productRepository.findAll(relaxedRating, pageable).getContent());
        }

        // Relax 3: bỏ luôn điều kiện giá và rating, chỉ giữ category/brand
        if (results.isEmpty() && (c.getMinPrice() != null || c.getMaxPrice() != null)) {
            Specification<Product> relaxedPrice = ProductSpecifications.search(
                    c.getKeyword(),
                    c.getCategorySlug(),
                    null,
                    null,
                    c.getBrand(),
                    null);
            results = new ArrayList<>(productRepository.findAll(relaxedPrice, pageable).getContent());
        }


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
                || (msgLower.contains("nó") && "product".equals(lastIntent))
                || (msgLower.length() < 20 && "product".equals(lastIntent)); // Câu rất ngắn trong ctx product
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
            case "PENDING" -> "⏳";
            case "CONFIRMED" -> "✅";
            case "SHIPPED" -> "🚚";
            case "DELIVERED" -> "✅";
            case "CANCELED" -> "❌";
            case "RETURN_REQUESTED" -> "🔄";
            default -> "📦";
        };
    }

    /**
     * Lưu lịch sử chat vào DB.
     * Dùng REQUIRES_NEW để transaction hoàn toàn độc lập, tránh bị nhiễm trạng thái
     * rollback-only
     * từ bất kỳ exception nào trong processMessage.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected void saveChatMessage(ChatRequest request, ChatResponse response) {
        try {
            // Lưu tin nhắn user
            chatMessageRepository.save(ChatMessage.builder()
                    .sessionId(request.getSessionId())
                    .message(request.getMessage())
                    .response("") // trường response của user turn = rỗng
                    .messageType(ChatMessage.MessageType.USER)
                    .userId(request.getUserId())
                    .build());

            // Lưu phản hồi bot
            chatMessageRepository.save(ChatMessage.builder()
                    .sessionId(request.getSessionId())
                    .message(request.getMessage())
                    .response(response.getResponse() != null ? response.getResponse() : "")
                    .messageType(ChatMessage.MessageType.BOT)
                    .intent(response.getIntent())
                    .userId(request.getUserId())
                    .build());

        } catch (Exception e) {
            // Không để lỗi DB lăn ra làm hỏng response trả về cho user
            log.error("[Chatbot] Failed to save chat message to DB: {}", e.getMessage());
        }
    }

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

        // Nếu là điện thoại Samsung / Apple mà keyword null, cứ để brand + category
        // query
        if (!hasText(c.getKeyword()) && "electronics".equals(c.getCategorySlug()) && hasText(c.getBrand())) {
            // Không cần ép keyword; brand + category đủ lọc catalog seed tốt hơn
        }

        // Nếu user nói "đồng hồ" thì vẫn giữ keyword vì nó có giá trị search
        if (!hasText(c.getKeyword()) && containsAny(userMessage, "đồng hồ", "smartwatch", "watch")) {
            c.setKeyword("đồng hồ");
            c.setCategorySlug("electronics");
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

        if (containsAny(msg, "iphone", "samsung", "galaxy", "apple", "điện thoại", "smartphone", "đồng hồ", "watch")) {
            return "electronics";
        }
        if (containsAny(msg, "áo", "váy", "giày", "hoodie", "sơ mi", "sneaker")) {
            return "fashion";
        }
        if (containsAny(msg, "ghế", "bàn", "nội thất")) {
            return "home-living";
        }
        if (containsAny(msg, "kem", "mặt nạ", "dưỡng ẩm", "mỹ phẩm", "skincare")) {
            return "beauty";
        }
        return null;
    }

    private String inferBrandFromText(String text) {
        String msg = safeLower(text);
        if (containsAny(msg, "samsung", "galaxy"))
            return "samsung";
        if (containsAny(msg, "apple", "iphone"))
            return "apple";
        return null;
    }

    private String inferKeywordFromText(String text, String categorySlug) {
        String msg = safeLower(text);

        if ("fashion".equals(categorySlug)) {
            if (msg.contains("áo"))
                return "áo";
            if (msg.contains("váy"))
                return "váy";
            if (msg.contains("giày"))
                return "giày";
        }

        if ("home-living".equals(categorySlug)) {
            if (msg.contains("ghế"))
                return "ghế";
            if (msg.contains("bàn"))
                return "bàn";
        }

        if ("beauty".equals(categorySlug)) {
            if (msg.contains("kem"))
                return "kem";
            if (msg.contains("mặt nạ"))
                return "mặt nạ";
        }

        if ("electronics".equals(categorySlug)) {
            if (msg.contains("đồng hồ"))
                return "đồng hồ";
            if (msg.contains("iphone"))
                return "iphone";
            if (msg.contains("galaxy"))
                return "galaxy";
            return null; // "điện thoại" là generic -> để null
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
}
