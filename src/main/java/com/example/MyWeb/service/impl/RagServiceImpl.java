package com.example.MyWeb.service.impl;

import com.example.MyWeb.model.FaqDocument;
import com.example.MyWeb.repository.FaqDocumentRepository;
import com.example.MyWeb.service.LlmService;
import com.example.MyWeb.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagServiceImpl implements RagService {

    private final LlmService llmService;
    private final FaqDocumentRepository documentRepository;

    // Ngưỡng cosine distance: 0 = giống hệt, 2 = hoàn toàn khác.
    // 0.65 nghĩa là chỉ lấy document đủ liên quan, tránh inject context sai.
    private static final double SIMILARITY_THRESHOLD = 0.65;

    @Override
    public List<FaqDocument> retrieveRelevantContext(String query, int topK) {
        log.info("RAG: Generating embedding for query: '{}'", query);
        
        // 1. Tạo vector cho câu hỏi (Dùng Gemini LLM)
        float[] queryVector = llmService.embed(query);
        
        // Đảm bảo vector không bị rỗng (do lỗi API)
        if (queryVector == null || queryVector.length == 0 || queryVector[0] == 0f) {
            log.warn("RAG: Query embedding failed. Returning empty context.");
            return List.of();
        }

        // 2. Format vector chuẩn để query PostgreSQL pgvector
        String vectorString = formatVectorForQuery(queryVector);

        // 3. Search DB với threshold để lọc kết quả không liên quan
        log.info("RAG: Retrieving top {} documents with distance < {}", topK, SIMILARITY_THRESHOLD);
        try {
            List<FaqDocument> results = documentRepository.findTopSimilarDocuments(
                    vectorString, topK, SIMILARITY_THRESHOLD);
            
            if (results.isEmpty()) {
                log.info("RAG: No documents within threshold {}. Bot will use fallback.", SIMILARITY_THRESHOLD);
            } else {
                log.info("RAG: Found {} relevant document(s): {}",
                        results.size(),
                        results.stream().map(FaqDocument::getTitle).toList());
            }
            return results;
        } catch (Exception e) {
            log.error("RAG pgvector search failed. Did you run 'CREATE EXTENSION vector;' in DB?", e);
            return List.of();
        }
    }

    @Override
    public String buildContextString(List<FaqDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "(Không tìm thấy chính sách nào liên quan)";
        }

        StringBuilder sb = new StringBuilder();
        for (FaqDocument doc : documents) {
            sb.append("- Chủ đề: ").append(doc.getTitle()).append("\n");
            sb.append("  Nội dung: ").append(doc.getContent()).append("\n\n");
        }
        return sb.toString().trim();
    }

    @Override
    public String formatVectorForQuery(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
