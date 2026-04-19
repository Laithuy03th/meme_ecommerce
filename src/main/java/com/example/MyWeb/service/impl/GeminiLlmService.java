package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.ChatTurn;
import com.example.MyWeb.service.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Triển khai LlmService sử dụng Google Gemini API (gemini-1.5-flash).
 *
 * Tài liệu API: https://ai.google.dev/api/generate-content
 * Lấy API key miễn phí: https://aistudio.google.com/app/apikey
 */
@Service
@Slf4j
public class GeminiLlmService implements LlmService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model.chat:gemini-1.5-flash}")
    private String chatModel;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String apiBaseUrl;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    // === System Prompt Chung ===
    private static final String SYSTEM_CONTEXT = """
            Bạn là trợ lý AI của cửa hàng thương mại điện tử MemeShop (MyWeb).
            - Luôn trả lời bằng tiếng Việt, thân thiện, ngắn gọn và hữu ích.
            - Chỉ tư vấn về sản phẩm, đơn hàng, chính sách của cửa hàng.
            - Không bịa đặt thông tin; nếu không biết, hãy nói thật.
            - Sử dụng emoji phù hợp để làm câu trả lời sinh động hơn.
            """;

    public GeminiLlmService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String classifyIntent(String userMessage, List<ChatTurn> history) {
        String contextSummary = buildContextSummary(history);

        String prompt = String.format("""
                %s
                
                === NGỮ CẢNH HỘI THOẠI GẦN ĐÂY ===
                %s
                
                === NHIỆM VỤ ===
                Phân loại câu hỏi sau vào ĐÚNG MỘT trong các nhóm:
                - "product"  : tìm kiếm sản phẩm, hỏi giá, so sánh, gợi ý mua hàng
                - "policy"   : chính sách đổi trả, giao hàng, bảo hành, thanh toán, voucher
                - "order"    : tra cứu đơn hàng, trạng thái đơn, lịch sử mua hàng
                - "greeting" : chào hỏi, hỏi thông tin cửa hàng, liên hệ
                - "other"    : không rõ ý định hoặc nằm ngoài các nhóm trên
                
                Câu hỏi: "%s"
                
                Chỉ trả lời DUY NHẤT một từ trong ngoặc kép, ví dụ: product
                Không giải thích thêm, không thêm dấu câu.
                """,
                SYSTEM_CONTEXT, contextSummary, userMessage);

        String result = callGeminiAPI(prompt, 20, false);
        // Cleanup: đảm bảo chỉ nhận đúng 1 trong 5 giá trị hợp lệ
        String cleaned = result.trim().toLowerCase()
                .replaceAll("[\"'`]", "") // Remove quotes
                .replaceAll("\\s+", "");  // Remove whitespace

        if (List.of("product", "policy", "order", "greeting", "other").contains(cleaned)) {
            log.info("Intent classified: '{}' → '{}'", userMessage.substring(0, Math.min(50, userMessage.length())), cleaned);
            return cleaned;
        }

        log.warn("Unexpected intent response from Gemini: '{}', defaulting to 'other'", cleaned);
        return "other";
    }

    @Override
    public String generateResponse(String systemPrompt, String userMessage, List<ChatTurn> history) {
        // Build proper Gemini multi-turn contents — đây là cách đúng thay vì nhồi history vào 1 text blob
        java.util.List<java.util.Map<String, Object>> contents =
                buildMultiTurnContents(history, systemPrompt, userMessage);
        return callGeminiMultiTurn(contents, 1024);
    }

    @Override
    public String extractProductConstraints(String userMessage, List<ChatTurn> history) {
        String contextSummary = buildContextSummary(history);

        String prompt = String.format("""
                %s
                
                === NGỮ CẢNH (Sản phẩm đã đề cập trước đó) ===
                %s
                
                === NHIỆM VỤ ===
                Phân tích câu hỏi tìm kiếm sản phẩm sau và trả về JSON với các trường CỰC KỲ CHÍNH XÁC:
                - "keyword": Từ khóa chung chung (ví dụ: "áo thun", "iphone"), null nếu không có
                - "categorySlug": Phải map vào 1 trong 4 danh mục ("electronics", "fashion", "home-living", "beauty"), nếu không rõ thì để null
                - "brand": Tên thương hiệu (ví dụ: "apple", "samsung", "asus", v.v.), null nếu không có
                - "maxPrice": Giá tối đa bằng số (VND, không có đơn vị), null nếu không có
                - "minPrice": Giá tối thiểu bằng số (VND), null nếu không có
                - "minRating": Số sao thấp nhất (ví dụ yêu cầu "tốt", "chất lượng" thì cho 4.0; 5 sao thì 5.0), null nếu không có
                - "isFollowUp": true nếu câu này hỏi tiếp về sản phẩm ở ngữ cảnh trên, false nếu tìm mới
                
                Lưu ý: 
                - "15 triệu" = 15000000; "7tr" = 7000000; "500k" = 500000.
                - Nếu khách hỏi "điện thoại" -> categorySlug = "electronics". "váy", "áo" -> "fashion".
                
                Câu hỏi: "%s"
                
                Trả về CHỈ JSON thuần túy, không markdown, không giải thích:
                """,
                SYSTEM_CONTEXT, contextSummary, userMessage);

        String jsonResult = callGeminiAPI(prompt, 512, true);
            // Strip markdown code blocks nếu Gemini trả về
        return jsonResult.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
    }

    @Override
    public float[] embed(String text) {
        // Sử dụng model gemini-embedding-001. Dimensions = 768.
        String url = String.format("%s/gemini-embedding-001:embedContent?key=%s", apiBaseUrl, apiKey);

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("model", "models/gemini-embedding-001");
                put("content", new java.util.HashMap<>() {{
                    put("parts", List.of(new java.util.HashMap<>() {{
                        put("text", text);
                    }}));
                }});
            }});
        } catch (Exception e) {
            log.error("Failed to serialize embedding request body", e);
            return new float[768];
        }

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, MediaType.get("application/json; charset=utf-8")))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("Gemini Embedding API error: HTTP {} — {}", response.code(), errorBody);
                return new float[768];
            }

            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode values = root.path("embedding").path("values");

            if (values.isArray()) {
                float[] vector = new float[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    vector[i] = (float) values.get(i).asDouble();
                }
                return vector;
            }

            log.error("Unexpected Gemini embedding response format");
            return new float[768];

        } catch (IOException e) {
            log.error("Failed to call Gemini Embedding API: {}", e.getMessage());
            return new float[768];
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Gọi Gemini API với prompt thuần text.
     *
     * @param prompt           Prompt đầy đủ
     * @param allowLongResponse  true = cho phép response dài (generate), false = cần ngắn (classify)
     */
    private String callGeminiAPI(String prompt, int maxTokens, boolean expectJson) {
        String url = String.format("%s/%s:generateContent?key=%s", apiBaseUrl, chatModel, apiKey);

        // Build request JSON body theo Gemini API format
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("contents", List.of(new java.util.HashMap<>() {{
                    put("role", "user");
                    put("parts", List.of(new java.util.HashMap<>() {{
                        put("text", prompt);
                    }}));
                }}));
                put("generationConfig", new java.util.HashMap<>() {{
                    put("temperature", maxTokens > 100 ? 0.7 : 0.1);
                    put("maxOutputTokens", maxTokens);
                    put("topP", 0.8);
                    if (expectJson) {
                        put("responseMimeType", "application/json");
                    }
                }});
                put("safetySettings", List.of(
                    new java.util.HashMap<>() {{
                        put("category", "HARM_CATEGORY_HARASSMENT");
                        put("threshold", "BLOCK_ONLY_HIGH");
                    }},
                    new java.util.HashMap<>() {{
                        put("category", "HARM_CATEGORY_HATE_SPEECH");
                        put("threshold", "BLOCK_ONLY_HIGH");
                    }}
                ));
            }});
        } catch (Exception e) {
            log.error("Failed to serialize Gemini request body", e);
            return getFallbackResponse();
        }

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, MediaType.get("application/json; charset=utf-8")))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("Gemini API error: HTTP {} — {}", response.code(), errorBody);
                if (response.code() == 429) {
                    return "Hệ thống AI đang xử lý quá nhiều yêu cầu cùng lúc (Quá tải). Bạn vui lòng đợi khoảng 1 phút rồi thử lại nhé! ⏳";
                }
                return getFallbackResponse();
            }

            String responseBody = response.body().string();
            return parseGeminiResponse(responseBody);

        } catch (IOException e) {
            log.error("Failed to call Gemini API: {}", e.getMessage());
            return getFallbackResponse();
        }
    }

    /**
     * Parse response JSON từ Gemini API.
     * Format: candidates[0].content.parts[0].text
     */
    private String parseGeminiResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isMissingNode() || !candidates.isArray() || candidates.size() == 0) {
                 log.error("Gemini returned no candidates. Full response: {}", jsonResponse);
                 return getFallbackResponse();
            }
            
            JsonNode firstCandidate = candidates.get(0);
            JsonNode content = firstCandidate.path("content");
            
            // Check for blocked content first
            JsonNode blockReason = firstCandidate.path("finishReason");
            if (blockReason != null && ("SAFETY".equals(blockReason.asText()) || "OTHER".equals(blockReason.asText()))) {
                log.warn("Gemini response blocked by safety filters or other finish reason. {}", blockReason.asText());
                return "Xin lỗi, tôi không thể trả lời câu hỏi này do nó vi phạm tiêu chuẩn cộng đồng. 🙏";
            }

            JsonNode text = content.path("parts").get(0).path("text");
            if (text != null && !text.isMissingNode()) {
                return text.asText().trim();
            }

            log.error("Unexpected Gemini response format: {}", jsonResponse.substring(0, Math.min(200, jsonResponse.length())));
            return getFallbackResponse();

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {} | JSON: {}", e.getMessage(), jsonResponse);
            return getFallbackResponse();
        }
    }

    /**
     * Tóm tắt ngữ cảnh hội thoại thành text ngắn gọn.
     */
    private String buildContextSummary(List<ChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return "(Không có ngữ cảnh trước)";
        }
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, history.size() - 4); // Lấy 4 turn gần nhất
        for (int i = start; i < history.size(); i++) {
            ChatTurn turn = history.get(i);
            sb.append(turn.getRole().equals("user") ? "User" : "Bot")
              .append(": ").append(turn.getContent()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Build conversation history theo format phù hợp với Gemini prompt.
     */
    private String buildConversationFormatted(List<ChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return "(Bắt đầu hội thoại mới)";
        }
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, history.size() - 6); // 6 turn gần nhất
        for (int i = start; i < history.size(); i++) {
            ChatTurn turn = history.get(i);
            sb.append(turn.getRole().equals("user") ? "User" : "Trợ lý")
              .append(": ").append(turn.getContent()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Build Gemini multi-turn contents array từ conversation history.
     * Gemini API yêu cầu contents là list các {role, parts} xen kẽ nhau (user/model).
     * Không được có 2 turn cùng role liên tiếp, và phải bắt đầu bằng user.
     */
    private java.util.List<java.util.Map<String, Object>> buildMultiTurnContents(
            List<ChatTurn> history, String taskPrompt, String userMessage) {

        java.util.List<java.util.Map<String, Object>> contents = new java.util.ArrayList<>();

        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 6); // tối đa 6 turn gần nhất
            // Gemini yêu cầu bắt đầu bằng role "user" — bỏ qua turn đầu nếu là model
            while (start < history.size() && "model".equals(history.get(start).getRole())) {
                start++;
            }
            for (int i = start; i < history.size(); i++) {
                ChatTurn turn = history.get(i);
                java.util.Map<String, Object> turnMap = new java.util.LinkedHashMap<>();
                turnMap.put("role", turn.getRole()); // "user" hoặc "model"
                turnMap.put("parts", java.util.List.of(java.util.Map.of("text", turn.getContent())));
                contents.add(turnMap);
            }
        }

        // Turn hiện tại: kết hợp task-specific systemPrompt + user message thành 1 user turn
        String currentText = (taskPrompt == null || taskPrompt.isBlank())
                ? userMessage
                : taskPrompt + "\n\n=== CÂU Hỏi CỦA KHÁCH HÀNG ===\n" + userMessage;

        java.util.Map<String, Object> currentTurn = new java.util.LinkedHashMap<>();
        currentTurn.put("role", "user");
        currentTurn.put("parts", java.util.List.of(java.util.Map.of("text", currentText)));
        contents.add(currentTurn);

        return contents;
    }

    /**
     * Gọi Gemini API với multi-turn conversation format đúng chuẩn.
     * Dùng systemInstruction riêng biệt (Gemini 1.5+) thay vì nhồi vào prompt text.
     */
    private String callGeminiMultiTurn(
            java.util.List<java.util.Map<String, Object>> contents, int maxTokens) {

        String url = String.format("%s/%s:generateContent?key=%s", apiBaseUrl, chatModel, apiKey);

        String requestBody;
        try {
            java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
            // systemInstruction: Gemini 1.5 xử lý tốt hơn so với nhồi vào prompt
            bodyMap.put("systemInstruction", java.util.Map.of(
                    "parts", java.util.List.of(java.util.Map.of("text", SYSTEM_CONTEXT))));
            bodyMap.put("contents", contents);
            bodyMap.put("generationConfig", new java.util.HashMap<>() {{
                put("temperature", 0.7);
                put("maxOutputTokens", maxTokens);
                put("topP", 0.8);
            }});
            bodyMap.put("safetySettings", java.util.List.of(
                    java.util.Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_ONLY_HIGH"),
                    java.util.Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_ONLY_HIGH")
            ));
            requestBody = objectMapper.writeValueAsString(bodyMap);
        } catch (Exception e) {
            log.error("Failed to serialize Gemini multi-turn request body", e);
            return getFallbackResponse();
        }

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, MediaType.get("application/json; charset=utf-8")))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("Gemini multi-turn API error: HTTP {} — {}", response.code(), errorBody);
                if (response.code() == 429) {
                    return "Ỉ ơi, hệ thống đang bận xử lý nhiều yêu cầu. Bạn đợi tôi một chút rồi thử lại nhé! ⏳";
                }
                return getFallbackResponse();
            }
            return parseGeminiResponse(response.body().string());
        } catch (IOException e) {
            log.error("Failed to call Gemini multi-turn API: {}", e.getMessage());
            return getFallbackResponse();
        }
    }

    private String getFallbackResponse() {
        return "Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau hoặc liên hệ support@myweb.com 🙏";
    }
}
