package com.example.MyWeb.service;

import com.example.MyWeb.dto.ChatRequest;
import com.example.MyWeb.dto.ChatResponse;
import com.example.MyWeb.dto.QuickReply;
import com.example.MyWeb.model.ChatMessage;
import com.example.MyWeb.model.ChatbotKnowledge;
import com.example.MyWeb.model.Order;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.repository.ChatMessageRepository;
import com.example.MyWeb.repository.ChatbotKnowledgeRepository;
import com.example.MyWeb.repository.OrderRepository;
import com.example.MyWeb.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatbotKnowledgeRepository knowledgeRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public ChatResponse processMessage(ChatRequest request) {
        log.info("Processing message: {} for session: {}", request.getMessage(), request.getSessionId());

        String userMessage = request.getMessage().trim().toLowerCase();

        // 1. Detect intent
        IntentResult intentResult = detectIntent(userMessage);

        // 2. Generate response based on intent
        ChatResponse response = generateResponse(intentResult, request);

        // 3. Save to database
        saveChatMessage(request, response);

        return response;
    }

    @Override
    public List<ChatResponse> getChatHistory(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        return messages.stream()
                .filter(msg -> msg.getMessageType() == ChatMessage.MessageType.BOT)
                .map(msg -> ChatResponse.builder()
                        .response(msg.getResponse())
                        .intent(msg.getIntent())
                        .sessionId(msg.getSessionId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getQuickStartSuggestions() {
        return Arrays.asList(
                "Tìm sản phẩm",
                "Kiểm tra đơn hàng",
                "Thanh toán như thế nào?",
                "Chính sách giao hàng",
                "Voucher giảm giá");
    }

    // ==================== Private Methods ====================

    private IntentResult detectIntent(String message) {
        List<ChatbotKnowledge> activeKnowledge = knowledgeRepository.findByIsActiveTrueOrderByPriorityDesc();

        // Self-Healing: Nếu knowledge base rỗng, tự động khởi tạo lại ngay lập tức
        if (activeKnowledge.isEmpty()) {
            log.warn("Knowledge base is empty during intent detection! Triggering initialization...");
            initializeKnowledgeBase();
            activeKnowledge = knowledgeRepository.findByIsActiveTrueOrderByPriorityDesc();
        }

        for (ChatbotKnowledge knowledge : activeKnowledge) {
            for (String pattern : knowledge.getPatterns()) {
                if (matchesPattern(message, pattern)) {
                    log.info("Matched intent: {} with pattern: {}", knowledge.getIntent(), pattern);
                    return new IntentResult(knowledge.getIntent(), knowledge, extractEntities(message));
                }
            }
        }

        log.info("No intent matched for message: {}", message);
        // Default fallback intent
        return new IntentResult("fallback", null, new HashMap<>());
    }

    private boolean matchesPattern(String message, String pattern) {
        if (message == null || pattern == null)
            return false;

        // Normalize
        String normalizedMsg = message.trim().toLowerCase();
        String normalizedPattern = pattern.trim().toLowerCase();

        // 1. Direct match (Exact equality)
        if (normalizedMsg.equals(normalizedPattern))
            return true;

        // 2. Wildcard regex match
        // Escape special regex chars except '*' and '?'
        String regex = "\\Q" + normalizedPattern.replace("*", "\\E.*\\Q").replace("?", "\\E.?\\Q") + "\\E";

        // Clean up empty Q-E pairs (optional but cleaner)
        regex = regex.replace("\\Q\\E", "");

        // Add boundary checks or flexible whitespace
        // Pattern: "tìm *" -> Regex: "tìm .*"
        Pattern p = Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(normalizedMsg);
        return m.find();
    }

    private Map<String, String> extractEntities(String message) {
        Map<String, String> entities = new HashMap<>();

        // Extract order ID (e.g., "đơn 123", "order 456", "#789")
        Pattern orderPattern = Pattern.compile("(?:đơn|order|#)\\s*(\\d+)");
        Matcher orderMatcher = orderPattern.matcher(message);
        if (orderMatcher.find()) {
            entities.put("orderId", orderMatcher.group(1));
        }

        // 1. Try hardcoded keywords first (High priority)
        String[] keywords = {
                // Fashion - Vietnamese
                "áo", "sơ mi", "khoác", "thun", "polo", "len", "vest",
                "quần", "jean", "tây", "short", "dài", "kaki",
                "váy", "đầm", "dạ hội", "công sở", "dự tiệc",
                "giày", "sneaker", "boot", "sandal", "dép", "cao gót",

                // Fashion - English (match DataSeeder)
                "dress", "shoes", "jacket", "sneakers", "boots",
                "t-shirt", "tshirt", "shirt", "loafers", "leather",

                // Electronics - Vietnamese
                "tai nghe", "loa", "đồng hồ", "smart watch",

                // Electronics - English (match DataSeeder)
                "headphones", "speaker", "watch", "wireless", "earbuds",
                "mouse", "keyboard", "charger", "router",

                // Beauty - Vietnamese
                "mỹ phẩm", "kem", "son", "phấn",

                // Beauty - English (match DataSeeder)
                "serum", "cream", "face", "moisturizer", "toner",
                "wash", "scrub", "mask", "mist",

                // Home & Living - Vietnamese
                "đèn", "ghế", "bàn", "nội thất",

                // Home & Living - English (match DataSeeder)
                "lamp", "chair", "desk", "furniture", "table"
        };
        boolean keywordFound = false;
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                entities.put("productKeyword", keyword);
                keywordFound = true;
                break; // Chỉ lấy keyword đầu tiên tìm thấy
            }
        }

        // 2. If no hardcoded keyword found, try to extract from "find/buy" pattern
        // Regex capture text after "tìm", "mua", "search"
        if (!keywordFound) {
            Pattern searchPattern = Pattern.compile("(?:tìm|mua|search|check)\\s+(.+)");
            Matcher searchMatcher = searchPattern.matcher(message);
            if (searchMatcher.find()) {
                String potentialKeyword = searchMatcher.group(1).trim();
                // Avoid capturing if it looks like an order check
                if (!potentialKeyword.matches(".*(?:đơn|order|#).*")) {
                    entities.put("productKeyword", potentialKeyword);
                }
            }
        }

        return entities;
    }

    private ChatResponse generateResponse(IntentResult intentResult, ChatRequest request) {
        String intent = intentResult.getIntent();
        ChatbotKnowledge knowledge = intentResult.getKnowledge();
        Map<String, String> entities = intentResult.getEntities();

        ChatResponse.ChatResponseBuilder responseBuilder = ChatResponse.builder()
                .intent(intent)
                .sessionId(request.getSessionId());

        switch (intent) {
            case "greeting":
                return handleGreeting(responseBuilder);

            case "product_inquiry":
                return handleProductInquiry(responseBuilder, entities);

            case "order_tracking":
                return handleOrderTracking(responseBuilder, entities, request.getUserId());

            case "payment_info":
                return handlePaymentInfo(responseBuilder);

            case "shipping_info":
                return handleShippingInfo(responseBuilder);

            case "voucher_info":
                return handleVoucherInfo(responseBuilder);

            case "contact":
                return handleContact(responseBuilder);

            case "fallback":
            default:
                return handleFallback(responseBuilder, knowledge);
        }
    }

    private ChatResponse handleGreeting(ChatResponse.ChatResponseBuilder builder) {
        String[] greetings = {
                "Xin chào! Tôi là trợ lý ảo của MyWeb. Tôi có thể giúp gì cho bạn?",
                "Chào bạn! Mình có thể hỗ trợ bạn về sản phẩm, đơn hàng, thanh toán. Bạn cần gì nhé?",
                "Hello! Rất vui được hỗ trợ bạn hôm nay. Bạn muốn tìm hiểu về điều gì?"
        };

        return builder
                .response(greetings[new Random().nextInt(greetings.length)])
                .quickReplies(Arrays.asList(
                        QuickReply.builder().label("Tìm sản phẩm").value("tìm sản phẩm").icon("🔍").build(),
                        QuickReply.builder().label("Kiểm tra đơn hàng").value("kiểm tra đơn hàng").icon("📦").build(),
                        QuickReply.builder().label("Thanh toán").value("thanh toán như thế nào").icon("💳").build(),
                        QuickReply.builder().label("Voucher").value("có voucher gì").icon("🎫").build()))
                .build();
    }

    private ChatResponse handleProductInquiry(ChatResponse.ChatResponseBuilder builder, Map<String, String> entities) {
        String keyword = entities.get("productKeyword");

        if (keyword != null) {
            // Search products by keyword
            List<Product> products = productRepository.findByNameContainingIgnoreCaseAndStatus(keyword, "ACTIVE");

            if (!products.isEmpty()) {
                List<Map<String, Object>> productData = products.stream()
                        .limit(5)
                        .map(p -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("id", p.getId());
                            data.put("name", p.getName());
                            data.put("price", p.getBasePrice());
                            data.put("imageUrl", p.getThumbnailUrl());
                            return data;
                        })
                        .collect(Collectors.toList());

                return builder
                        .response(String.format("Tìm thấy %d sản phẩm phù hợp với '%s':", products.size(), keyword))
                        .data(Map.of("products", productData))
                        .quickReplies(Arrays.asList(
                                QuickReply.builder().label("Xem tất cả").value("xem tất cả " + keyword).build(),
                                QuickReply.builder().label("Tìm sản phẩm khác").value("tìm sản phẩm khác").build()))
                        .build();
            }
        }

        // Default product inquiry response
        return builder
                .response("Bạn muốn tìm sản phẩm gì? Hãy cho mình biết tên hoặc loại sản phẩm bạn quan tâm nhé! 😊")
                .quickReplies(Arrays.asList(
                        QuickReply.builder().label("Sản phẩm mới").value("sản phẩm mới").build(),
                        QuickReply.builder().label("Sản phẩm bán chạy").value("sản phẩm bán chạy").build(),
                        QuickReply.builder().label("Khuyến mãi").value("khuyến mãi").build()))
                .build();
    }

    private ChatResponse handleOrderTracking(ChatResponse.ChatResponseBuilder builder,
            Map<String, String> entities,
            Long userId) {
        String orderIdStr = entities.get("orderId");

        // CASE 1: Người dùng đã login nhưng KHÔNG nhập mã đơn cụ thể -> Xem danh sách
        // đơn hàng
        if (orderIdStr == null && userId != null) {
            Pageable pageable = PageRequest.of(0, 5);
            Page<Order> page = orderRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);

            if (page.isEmpty()) {
                return builder.response("Bạn chưa có đơn hàng nào tại MyWeb. 🛍️\nHãy dạo một vòng xem sản phẩm nhé!")
                        .quickReplies(
                                Arrays.asList(QuickReply.builder().label("Xem sản phẩm").value("tìm sản phẩm").build()))
                        .build();
            }

            // Tạo data structure để FE render được
            List<Map<String, Object>> ordersData = page.getContent().stream()
                    .map(o -> {
                        Map<String, Object> orderMap = new HashMap<>();
                        orderMap.put("id", o.getId());
                        orderMap.put("status", o.getStatus().toString());
                        orderMap.put("paymentStatus",
                                o.getPaymentStatus() != null ? o.getPaymentStatus().toString() : "UNKNOWN");
                        orderMap.put("totalAmount", o.getTotalAmount());
                        orderMap.put("createdAt", o.getCreatedAt());
                        orderMap.put("itemCount", o.getItems() != null ? o.getItems().size() : 0);
                        return orderMap;
                    })
                    .collect(Collectors.toList());

            StringBuilder msg = new StringBuilder("📦 **Đơn hàng gần đây của bạn:**\n\n");
            for (Order o : page.getContent()) {
                msg.append(String.format("• #%d - %s (%s)\n",
                        o.getId(),
                        o.getStatus(),
                        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(o.getTotalAmount())));
            }
            msg.append("\n💡 Nhấn vào đơn hàng để xem chi tiết!");

            List<QuickReply> replies = page.getContent().stream()
                    .limit(3)
                    .map(o -> QuickReply.builder()
                            .label("Xem đơn #" + o.getId())
                            .value("đơn " + o.getId())
                            .icon("📦")
                            .build())
                    .collect(Collectors.toList());

            // Thêm .data() chứa thông tin đơn hàng để FE render UI
            return builder
                    .response(msg.toString())
                    .data(Map.of("orders", ordersData, "totalOrders", page.getTotalElements()))
                    .quickReplies(replies)
                    .build();
        }

        // CASE 2: Tra cứu đơn hàng cụ thể theo ID (như cũ)
        if (orderIdStr != null) {
            try {
                Long orderId = Long.parseLong(orderIdStr);
                Optional<Order> orderOpt = orderRepository.findById(orderId);

                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();

                    // Security check
                    if (userId == null) {
                        return builder
                                .response(
                                        "Bạn cần **đăng nhập** để xem chi tiết đơn hàng này. Vui lòng đăng nhập và thử lại nhé! 🔒")
                                .requiresAuth(true)
                                .build();
                    }

                    if (!order.getUser().getId().equals(userId)) {
                        return builder
                                .response("⚠️ Đơn hàng #" + orderId
                                        + " không thuộc về tài khoản của bạn. Vui lòng kiểm tra lại mã đơn hàng.")
                                .build();
                    }

                    String statusMessage = getOrderStatusMessage(order);

                    Map<String, Object> orderData = new HashMap<>();
                    orderData.put("orderId", order.getId());
                    orderData.put("status", order.getStatus());
                    orderData.put("paymentStatus", order.getPaymentStatus());
                    orderData.put("totalAmount", order.getTotalAmount());
                    orderData.put("createdAt", order.getCreatedAt());

                    return builder
                            .response(statusMessage)
                            .data(Map.of("order", orderData))
                            .quickReplies(Arrays.asList(
                                    QuickReply.builder().label("Xem chi tiết").value("chi tiết đơn " + orderId).build(),
                                    QuickReply.builder().label("Hủy đơn hàng").value("hủy đơn " + orderId).build()))
                            .build();
                } else {
                    return builder
                            .response("Không tìm thấy đơn hàng #" + orderId + ". Vui lòng kiểm tra lại mã đơn hàng. 🔍")
                            .build();
                }
            } catch (NumberFormatException e) {
                return builder
                        .response("Mã đơn hàng không hợp lệ. Vui lòng nhập số đơn hàng. 📝")
                        .build();
            }
        }

        // CASE 3: Chưa login và không có ID
        return builder
                .response(
                        "Vui lòng **đăng nhập** để xem lịch sử đơn hàng, hoặc cung cấp mã đơn (VD: 'đơn 123') để tra cứu nhanh. 🔒")
                .requiresAuth(userId == null)
                .build();
    }

    private String getOrderStatusMessage(Order order) {
        String baseMessage = String.format("Đơn hàng #%d của bạn: ", order.getId());

        switch (order.getStatus()) {
            case PENDING:
                return baseMessage + "đang chờ xác nhận. ⏳";
            case CONFIRMED:
                return baseMessage + "đã được xác nhận và đang chuẩn bị. 📦";
            case PACKED:
                return baseMessage + "đã đóng gói, sẵn sàng giao. 📦";
            case SHIPPED:
                return baseMessage + "đang trên đường giao đến bạn. 🚚";
            case DELIVERED:
                return baseMessage + "đã giao thành công. ✅";
            case CANCELED:
                return baseMessage + "đã bị hủy. ❌";
            case RETURN_REQUESTED:
                return baseMessage + "đang yêu cầu trả hàng. 🔄";
            case RETURNED:
                return baseMessage + "đã trả hàng. ↩️";
            case REFUNDED:
                return baseMessage + "đã hoàn tiền. 💰";
            default:
                return baseMessage + "đang được xử lý.";
        }
    }

    private ChatResponse handlePaymentInfo(ChatResponse.ChatResponseBuilder builder) {
        String response = "💳 **Phương thức thanh toán của MyWeb:**\n\n" +
                "✅ **COD** - Thanh toán khi nhận hàng\n" +
                "✅ **VNPay** - Thanh toán qua thẻ ATM/Visa/MasterCard/QR Code\n\n" +
                "Tất cả đều an toàn và bảo mật! 🔒";

        return builder
                .response(response)
                .quickReplies(Arrays.asList(
                        QuickReply.builder().label("Hướng dẫn thanh toán VNPay").value("hướng dẫn vnpay").build(),
                        QuickReply.builder().label("Thanh toán COD").value("cod là gì").build()))
                .build();
    }

    private ChatResponse handleShippingInfo(ChatResponse.ChatResponseBuilder builder) {
        String response = "🚚 **Chính sách giao hàng:**\n\n" +
                "📦 **Miễn phí** giao hàng đơn từ 500.000đ\n" +
                "⏱️ **Giao nhanh** trong 2-3 ngày\n" +
                "🏙️ Nội thành: 1-2 ngày\n" +
                "🌏 Ngoại thành: 3-5 ngày\n\n" +
                "Kiểm tra hàng trước khi thanh toán (COD)!";

        return builder
                .response(response)
                .quickReplies(Arrays.asList(
                        QuickReply.builder().label("Phí ship").value("phí ship bao nhiêu").build(),
                        QuickReply.builder().label("Thời gian giao").value("giao hàng bao lâu").build()))
                .build();
    }

    private ChatResponse handleVoucherInfo(ChatResponse.ChatResponseBuilder builder) {
        String response = "🎫 **Voucher giảm giá:**\n\n" +
                "💰 Chúng mình thường xuyên có các chương trình khuyến mãi!\n" +
                "🔔 Đăng ký nhận tin để không bỏ lỡ voucher mới nhất\n" +
                "🎁 Tích điểm mua sắm để đổi voucher\n\n" +
                "Bạn có thể xem voucher khả dụng trong giỏ hàng khi thanh toán nhé!";

        return builder
                .response(response)
                .quickReplies(Arrays.asList(
                        QuickReply.builder().label("Xem voucher").value("voucher hiện có").build(),
                        QuickReply.builder().label("Cách sử dụng").value("cách dùng voucher").build()))
                .build();
    }

    private ChatResponse handleContact(ChatResponse.ChatResponseBuilder builder) {
        String response = "📞 **Liên hệ với chúng mình:**\n\n" +
                "📧 Email: support@myweb.com\n" +
                "📱 Hotline: 1900-xxxx\n" +
                "⏰ Làm việc: 8:00 - 22:00 (T2-CN)\n\n" +
                "Hoặc bạn có thể để lại tin nhắn, team sẽ phản hồi sớm nhất!";

        return builder
                .response(response)
                .build();
    }

    private ChatResponse handleFallback(ChatResponse.ChatResponseBuilder builder, ChatbotKnowledge knowledge) {
        if (knowledge != null && knowledge.getResponses() != null && !knowledge.getResponses().isEmpty()) {
            // Get random response from knowledge base
            List<String> responses = knowledge.getResponses();
            String response = responses.get(new Random().nextInt(responses.size()));
            return builder.response(response).build();
        }

        // Default fallback
        String response = "Xin lỗi, mình chưa hiểu rõ câu hỏi của bạn. 😅\n" +
                "Bạn có thể hỏi mình về:\n" +
                "• Sản phẩm\n" +
                "• Đơn hàng\n" +
                "• Thanh toán\n" +
                "• Giao hàng\n" +
                "• Voucher";

        return builder
                .response(response)
                .quickReplies(Arrays.asList(
                        QuickReply.builder().label("Tìm sản phẩm").value("tìm sản phẩm").build(),
                        QuickReply.builder().label("Kiểm tra đơn hàng").value("kiểm tra đơn hàng").build(),
                        QuickReply.builder().label("Liên hệ").value("liên hệ").build()))
                .build();
    }

    private void saveChatMessage(ChatRequest request, ChatResponse response) {
        try {
            // Save user message
            ChatMessage userMessage = ChatMessage.builder()
                    .sessionId(request.getSessionId())
                    .message(request.getMessage())
                    .response("")
                    .messageType(ChatMessage.MessageType.USER)
                    .userId(request.getUserId())
                    .build();
            chatMessageRepository.save(userMessage);

            // Save bot response
            ChatMessage botMessage = ChatMessage.builder()
                    .sessionId(request.getSessionId())
                    .message(request.getMessage())
                    .response(response.getResponse())
                    .messageType(ChatMessage.MessageType.BOT)
                    .intent(response.getIntent())
                    .userId(request.getUserId())
                    .build();
            chatMessageRepository.save(botMessage);

        } catch (Exception e) {
            log.error("Error saving chat message", e);
        }
    }

    @PostConstruct
    @Override
    public void initializeKnowledgeBase() {
        // Check if knowledge base is empty
        if (knowledgeRepository.count() > 0) {
            log.info("Knowledge base already initialized");
            return;
        }

        log.info("Initializing chatbot knowledge base...");

        // Greeting
        knowledgeRepository.save(ChatbotKnowledge.builder()
                .intent("greeting")
                .patterns(Arrays.asList("xin chào", "hello", "hi", "chào", "hey", "chào bạn"))
                .responses(Arrays.asList("Xin chào! Tôi có thể giúp gì cho bạn?"))
                .priority(10)
                .build());

        // Product inquiry
        knowledgeRepository.save(ChatbotKnowledge.builder()
                .intent("product_inquiry")
                .patterns(Arrays.asList(
                        "tìm sản phẩm", "tìm *", "có * không", "* giá bao nhiêu",
                        "sản phẩm *", "mua *", "xem *"))
                .responses(Arrays.asList("Để tìm sản phẩm, hãy cho mình biết tên hoặc loại sản phẩm bạn cần!"))
                .requiresData(true)
                .dataSource("products")
                .priority(8)
                .build());

        // Order tracking
        knowledgeRepository.save(ChatbotKnowledge.builder()
                .intent("order_tracking")
                .patterns(Arrays.asList(
                        "đơn hàng của tôi", "lịch sử đơn hàng", "xem đơn hàng", "đơn hàng",
                        "đơn hàng *", "kiểm tra đơn *", "đơn *",
                        "order *", "track *", "theo dõi đơn"))
                .responses(Arrays.asList("Vui lòng cung cấp mã đơn hàng để kiểm tra."))
                .requiresData(true)
                .dataSource("orders")
                .priority(9)
                .build());

        // Payment info
        knowledgeRepository.save(ChatbotKnowledge.builder()
                .intent("payment_info")
                .patterns(Arrays.asList(
                        "thanh toán *", "payment *", "phương thức *",
                        "trả tiền *", "cod *", "vnpay *", "momo *"))
                .responses(Arrays.asList("Chúng mình hỗ trợ COD, VNPay và Momo!"))
                .priority(7)
                .build());

        // Shipping info
        knowledgeRepository.save(ChatbotKnowledge.builder()
                .intent("shipping_info")
                .patterns(Arrays.asList(
                        "giao hàng *", "ship *", "vận chuyển *",
                        "phí ship *", "delivery *"))
                .responses(Arrays.asList("Miễn phí ship đơn từ 500k, giao trong 2-3 ngày!"))
                .priority(7)
                .build());

        // Voucher info
        knowledgeRepository.save(ChatbotKnowledge.builder()
                .intent("voucher_info")
                .patterns(Arrays.asList(
                        "voucher *", "mã giảm giá *", "khuyến mãi *",
                        "giảm giá *", "coupon *"))
                .responses(Arrays.asList("Bạn có thể xem voucher khả dụng trong giỏ hàng!"))
                .priority(6)
                .build());

        // Contact
        knowledgeRepository.save(ChatbotKnowledge.builder()
                .intent("contact")
                .patterns(Arrays.asList(
                        "liên hệ *", "contact *", "hotline *",
                        "email *", "gọi *", "support *"))
                .responses(Arrays.asList("Email: support@myweb.com, Hotline: 1900-xxxx"))
                .priority(5)
                .build());

        log.info("Knowledge base initialized successfully!");
    }

    // Inner class for intent detection result
    private static class IntentResult {
        private final String intent;
        private final ChatbotKnowledge knowledge;
        private final Map<String, String> entities;

        public IntentResult(String intent, ChatbotKnowledge knowledge, Map<String, String> entities) {
            this.intent = intent;
            this.knowledge = knowledge;
            this.entities = entities;
        }

        public String getIntent() {
            return intent;
        }

        public ChatbotKnowledge getKnowledge() {
            return knowledge;
        }

        public Map<String, String> getEntities() {
            return entities;
        }
    }
}
