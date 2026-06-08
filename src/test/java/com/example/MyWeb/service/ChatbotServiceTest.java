package com.example.MyWeb.service;

import com.example.MyWeb.dto.ChatRequest;
import com.example.MyWeb.dto.ChatResponse;
import com.example.MyWeb.model.ChatMessage;
import com.example.MyWeb.model.FaqDocument;
import com.example.MyWeb.model.Order;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.model.User;
import com.example.MyWeb.model.enums.OrderStatus;
import com.example.MyWeb.model.enums.PaymentStatus;
import com.example.MyWeb.model.enums.PaymentMethod;
import com.example.MyWeb.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit Test: ChatbotServiceImpl
 *
 * Phạm vi kiểm thử:
 *  - TC-AI-01: Intent PRODUCT → tìm được sản phẩm trong DB
 *  - TC-AI-02: Intent POLICY → RAG tìm được FAQ, LLM tổng hợp
 *  - TC-AI-03: Intent ORDER → tra đơn hàng theo userId
 *  - TC-AI-04: Câu hỏi ngoài scope → trả fallback lịch sự (intent "other")
 *  - TC-AI-05: RAG không tìm thấy context → không bịa chính sách
 *  - TC-AI-06: RAG tìm được sản phẩm → prompt chứa context
 *  - TC-AI-07: LLM/Gemini ném exception → trả fallback response
 *  - TC-AI-08: User message rỗng → ném hoặc trả lỗi hợp lệ
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TC-AI: Kiểm thử ChatbotService – Intent & RAG Pipeline")
class ChatbotServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatHistoryService chatHistoryService;
    @Mock private ChatbotKnowledgeRepository knowledgeRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private LlmService llmService;
    @Mock private RagService ragService;
    @Mock private ConversationContextService contextService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ChatbotServiceImpl chatbotService;

    private static final Long USER_ID     = 1L;
    private static final String SESSION   = "sess-unit-test-001";

    private User     testUser;
    private Product  testProduct;
    private Order    testOrder;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(USER_ID).email("user@memeshop.vn").build();

        testProduct = Product.builder()
                .id(1L)
                .name("Áo phông MemeShop Classic")
                .basePrice(250_000.0)
                .brand("MemeShop")
                .slug("ao-phong-memeshop-classic")
                .averageRating(4.5)
                .build();

        testOrder = Order.builder()
                .id(100L)
                .user(testUser)
                .status(OrderStatus.SHIPPED)
                .paymentStatus(PaymentStatus.PAID)
                .paymentMethod(PaymentMethod.VNPAY)
                .totalAmount(300_000.0)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now())
                .items(Collections.emptyList())
                .build();

        // Default stubs
        when(contextService.getHistory(anyString())).thenReturn(Collections.emptyList());
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(anyString()))
                .thenReturn(Collections.emptyList());
        doNothing().when(chatHistoryService).saveChatMessage(any(), any());
    }

    // =========================================================================
    // TC-AI-01: Intent PRODUCT → tìm sản phẩm trong DB
    // =========================================================================
    @Test
    @DisplayName("TC-AI-01: Câu hỏi tư vấn sản phẩm → intent 'product', LLM được gọi với context sản phẩm")
    void processMessage_productIntent_shouldReturnProductAdvice() {
        // Arrange
        String message = "cho tôi xem áo phông";
        ChatRequest req = buildChatRequest(message, null);

        // LLM phân loại intent = "product"
        when(llmService.classifyIntent(anyString(), anyList()))
                .thenReturn("product");

        // DB trả về 1 sản phẩm
        Page<Product> productPage = new PageImpl<>(List.of(testProduct));
        when(productRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class)))
                .thenReturn(productPage);

        // LLM generate response
        when(llmService.generateResponse(contains("DANH SÁCH SẢN PHẨM"), anyString(), anyList()))
                .thenReturn("Đây là áo phông MemeShop Classic, giá 250.000 VNĐ, đánh giá 4.5★ rất tốt ạ!");

        // Act
        ChatResponse response = chatbotService.processMessage(req);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getIntent()).isEqualTo("product");
        assertThat(response.getResponse()).isNotBlank();

        // Verify LLM được gọi với danh sách sản phẩm
        verify(llmService, atLeastOnce())
                .generateResponse(contains("SẢN PHẨM"), anyString(), anyList());
    }

    // =========================================================================
    // TC-AI-02: Intent POLICY → RAG tìm thấy FAQ
    // =========================================================================
    @Test
    @DisplayName("TC-AI-02: Câu hỏi chính sách đổi trả → RAG truy xuất FAQ, LLM tổng hợp")
    void processMessage_policyIntent_shouldCallRagAndReturnAnswer() {
        // Arrange
        String message = "chính sách đổi trả hàng như thế nào?";
        ChatRequest req = buildChatRequest(message, null);

        when(llmService.classifyIntent(anyString(), anyList()))
                .thenReturn("policy");

        FaqDocument faqDoc = FaqDocument.builder()
                .id(1L)
                .title("Chính sách đổi trả hàng")
                .content("Khách hàng được đổi trả trong vòng 7 ngày kể từ ngày nhận hàng...")
                .build();

        when(ragService.retrieveRelevantContext(anyString(), anyInt()))
                .thenReturn(List.of(faqDoc));
        when(ragService.buildContextString(anyList()))
                .thenReturn("- Chủ đề: Chính sách đổi trả hàng\n  Nội dung: Khách hàng được đổi trả trong vòng 7 ngày...");
        when(llmService.generateResponse(contains("NGỮ CẢNH CHÍNH SÁCH"), anyString(), anyList()))
                .thenReturn("Theo chính sách của MemeShop, bạn có thể đổi trả trong 7 ngày kể từ ngày nhận hàng.");

        // Act
        ChatResponse response = chatbotService.processMessage(req);

        // Assert
        assertThat(response.getIntent()).isEqualTo("policy");
        assertThat(response.getResponse()).contains("7 ngày");

        // Verify RAG được gọi đúng
        verify(ragService, times(1)).retrieveRelevantContext(eq(message), anyInt());
        verify(ragService, times(1)).buildContextString(anyList());
    }

    // =========================================================================
    // TC-AI-03: Intent ORDER → tra đơn hàng theo userId
    // =========================================================================
    @Test
    @DisplayName("TC-AI-03: Hỏi trạng thái đơn hàng (đã login) → hiển thị danh sách đơn hàng")
    void processMessage_orderIntent_withLoggedInUser_shouldReturnOrderList() {
        // Arrange
        String message = "xem đơn hàng của tôi";
        ChatRequest req = buildChatRequest(message, USER_ID);

        when(llmService.classifyIntent(anyString(), anyList()))
                .thenReturn("order");

        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findByUser_IdOrderByCreatedAtDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(orderPage);

        when(llmService.generateResponse(contains("DANH SÁCH ĐƠN HÀNG"), anyString(), anyList()))
                .thenReturn("Bạn có 1 đơn hàng đang giao: Đơn #100, tổng 300.000đ.");

        // Act
        ChatResponse response = chatbotService.processMessage(req);

        // Assert
        assertThat(response.getIntent()).isEqualTo("order");
        assertThat(response.getResponse()).contains("đơn");
        assertThat(response.getData()).isNotNull();

        verify(orderRepository, atLeastOnce())
                .findByUser_IdOrderByCreatedAtDesc(eq(USER_ID), any(Pageable.class));
    }

    // =========================================================================
    // TC-AI-04: Câu hỏi ngoài scope → fallback (intent "other")
    // =========================================================================
    @Test
    @DisplayName("TC-AI-04: Câu hỏi ngoài phạm vi (hỏi thời tiết) → intent 'other', từ chối lịch sự")
    void processMessage_unrelatedQuestion_shouldReturnFallback() {
        // Arrange – LLM phân loại là "other"
        String message = "hôm nay thời tiết Hà Nội thế nào?";
        ChatRequest req = buildChatRequest(message, null);

        when(llmService.classifyIntent(anyString(), anyList()))
                .thenReturn("other");

        // Act
        ChatResponse response = chatbotService.processMessage(req);

        // Assert – intent là "other"
        assertThat(response.getIntent()).isEqualTo("other");

        // Fallback không gọi LLM để tránh hallucination (hardcoded)
        verify(llmService, never()).generateResponse(anyString(), anyString(), anyList());
    }

    // =========================================================================
    // TC-AI-05: RAG không tìm thấy FAQ → không bịa chính sách
    // =========================================================================
    @Test
    @DisplayName("TC-AI-05: RAG không tìm thấy FAQ liên quan → trả hướng dẫn, không gọi LLM bịa")
    void processMessage_policyIntent_ragNoResult_shouldNotHallucinate() {
        // Arrange – RAG trả rỗng
        String message = "bảo hành điện máy có áp dụng không?";
        ChatRequest req = buildChatRequest(message, null);

        when(llmService.classifyIntent(anyString(), anyList()))
                .thenReturn("policy");
        when(ragService.retrieveRelevantContext(anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        // Act
        ChatResponse response = chatbotService.processMessage(req);

        // Assert – không gọi LLM khi RAG rỗng (tránh hallucinate)
        assertThat(response.getResponse()).isNotBlank();

        verify(llmService, never())
                .generateResponse(contains("NGỮ CẢNH CHÍNH SÁCH"), anyString(), anyList());
    }

    // =========================================================================
    // TC-AI-06: RAG tìm được sản phẩm liên quan → prompt chứa context
    // =========================================================================
    @Test
    @DisplayName("TC-AI-06: Hỏi chính sách → RAG tìm được 3 docs → LLM nhận đủ context")
    void processMessage_policyIntent_ragFindsContext_shouldPassContextToLlm() {
        // Arrange – RAG trả 3 docs
        String message = "phương thức thanh toán có những loại nào?";
        ChatRequest req = buildChatRequest(message, null);

        when(llmService.classifyIntent(anyString(), anyList()))
                .thenReturn("policy");

        List<FaqDocument> docs = List.of(
                FaqDocument.builder().id(1L).title("Thanh toán COD").content("Khách trả tiền khi nhận hàng...").build(),
                FaqDocument.builder().id(2L).title("Thanh toán VNPay").content("Thanh toán online qua VNPay...").build(),
                FaqDocument.builder().id(3L).title("Ví điện tử").content("Hỗ trợ MoMo, ZaloPay...").build()
        );

        when(ragService.retrieveRelevantContext(anyString(), anyInt())).thenReturn(docs);
        when(ragService.buildContextString(docs)).thenReturn("- COD\n- VNPay\n- MoMo");
        when(llmService.generateResponse(anyString(), anyString(), anyList()))
                .thenReturn("MemeShop hỗ trợ 3 hình thức: COD, VNPay và ví điện tử (MoMo, ZaloPay).");

        // Act
        ChatResponse response = chatbotService.processMessage(req);

        // Assert – RAG context được dùng và LLM được gọi đúng
        verify(ragService).retrieveRelevantContext(eq(message), eq(3));
        verify(llmService).generateResponse(
                argThat(prompt -> prompt.contains("NGỮ CẢNH CHÍNH SÁCH")),
                eq(message), anyList());
    }

    // =========================================================================
    // TC-AI-07: Gemini API lỗi → trả fallback response không crash
    // =========================================================================
    @Test
    @DisplayName("TC-AI-07: Gemini API ném exception → trả về fallback response thân thiện")
    void processMessage_whenLlmThrowsException_shouldReturnFallbackResponse() {
        // Arrange
        String message = "tôi muốn tìm áo khoác";
        ChatRequest req = buildChatRequest(message, null);

        when(llmService.classifyIntent(anyString(), anyList()))
                .thenReturn("product");

        // DB trả sản phẩm
        Page<Product> productPage = new PageImpl<>(List.of(testProduct));
        when(productRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class)))
                .thenReturn(productPage);

        // LLM ném exception
        when(llmService.generateResponse(anyString(), anyString(), anyList()))
                .thenThrow(new RuntimeException("Gemini API timeout"));

        // Act – không được crash
        ChatResponse response = chatbotService.processMessage(req);

        // Assert – trả fallback thay vì throw
        assertThat(response).isNotNull();
        assertThat(response.getResponse()).isNotBlank();
        // Fallback phải thân thiện, không lộ stack trace
        assertThat(response.getResponse()).doesNotContain("Exception");
        assertThat(response.getResponse()).doesNotContain("RuntimeException");
    }

    // =========================================================================
    // TC-AI-08: Message rỗng → ném NullPointerException hoặc được xử lý gracefully
    // =========================================================================
    @Test
    @DisplayName("TC-AI-08: Message rỗng hoặc chỉ khoảng trắng → không crash, trả response hợp lệ")
    void processMessage_withBlankMessage_shouldHandleGracefully() {
        // Arrange – message chỉ có khoảng trắng
        ChatRequest req = buildChatRequest("   ", null);

        when(llmService.classifyIntent(anyString(), anyList()))
                .thenReturn("other");

        // Act – gọi trim() trong processMessage nên không crash
        ChatResponse response = chatbotService.processMessage(req);

        // Assert – phải trả về response hợp lệ
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo(SESSION);
    }

    // =========================================================================
    // Helper
    // =========================================================================
    private ChatRequest buildChatRequest(String message, Long userId) {
        ChatRequest req = new ChatRequest();
        req.setSessionId(SESSION);
        req.setMessage(message);
        req.setUserId(userId);
        return req;
    }
}
