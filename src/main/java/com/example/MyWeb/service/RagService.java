package com.example.MyWeb.service;

import com.example.MyWeb.model.FaqDocument;

import java.util.List;

/**
 * Service quản lý quá trình Retrieval-Augmented Generation (RAG).
 * Cho phép index tài liệu mới vào Vector DB và lấy ra các tài liệu phù hợp (Semantic Search).
 */
public interface RagService {
    
    /**
     * Tìm kiếm top K documents liên quan nhất đến câu query.
     */
    List<FaqDocument> retrieveRelevantContext(String query, int topK);
    
    /**
     * Lấy các context documents và build thành string để bỏ vào prompt cho LLM.
     */
    String buildContextString(List<FaqDocument> documents);
    
    /**
     * Helper tạo string định dạng (format) DB array phù hợp để query bằng pgvector trong native query.
     * "[1.2, 0.5, ...]" -> "[1.2,0.5,...]"
     */
    String formatVectorForQuery(float[] vector);
}
