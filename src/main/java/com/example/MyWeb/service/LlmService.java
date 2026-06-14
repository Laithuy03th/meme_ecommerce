package com.example.MyWeb.service;

import com.example.MyWeb.dto.ChatTurn;

import java.util.List;

/**
 * Interface cho LLM Service - trừu tượng hóa việc gọi Google Gemini API.
 */
public interface LlmService {

    /**
     * Phân loại intent từ câu hỏi của user.
     * Trả về một trong các giá trị: "product", "policy", "order", "greeting",
     * "other"
     *
     * @param userMessage Câu hỏi của user
     * @param history     Lịch sử hội thoại gần nhất (để hiểu ngữ cảnh)
     * @return Intent string
     */
    String classifyIntent(String userMessage, List<ChatTurn> history);

    /**
     * Tổng hợp câu trả lời cuối cùng bằng ngôn ngữ tự nhiên.
     *
     * @param systemPrompt Hướng dẫn cho LLM (vai trò, dữ liệu context, nguyên tắc)
     * @param userMessage  Câu hỏi của user
     * @param history      Lịch sử hội thoại để maintain context
     * @return Câu trả lời tự nhiên
     */
    String generateResponse(String systemPrompt, String userMessage, List<ChatTurn> history);

    /**
     * Trích xuất thông tin tìm kiếm sản phẩm từ câu hỏi tự nhiên.
     * Ví dụ: "Laptop dưới 15 triệu cho sinh viên IT" →
     * {"category":"laptop","maxPrice":15000000,"keywords":["sinh viên","IT"]}
     *
     * @param userMessage Câu hỏi tìm kiếm sản phẩm
     * @return JSON string chứa các constraint
     */
    String extractProductConstraints(String userMessage, List<ChatTurn> history);

    /**
     * Tạo vector embedding (768 chiều) từ đoạn text.
     * Cần thiết để lưu và tìm kiếm bằng pgvector (RAG).
     *
     * @param text Dữ liệu đầu vào (VD: nội dung FAQ)
     * @return Vector biểu diễn ý nghĩa của text
     */
    float[] embed(String text);
}
