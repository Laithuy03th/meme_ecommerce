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

    private static final double SIMILARITY_THRESHOLD = 1.2;

    private static final int EXPECTED_EMBEDDING_DIM = 3072;

    @Override
    public List<FaqDocument> retrieveRelevantContext(String query, int topK) {
        log.info("RAG: Generating embedding for query: '{}'", query);

        // 1. Tạo vector cho câu hỏi (Dùng Gemini LLM)
        float[] queryVector = llmService.embed(query);

        // Kiểm tra embedding hợp lệ và đúng dimension
        if (queryVector == null || queryVector.length == 0) {
            log.warn("RAG: Query embedding failed (null/empty). Returning empty context.");
            return List.of();
        }
        if (queryVector.length != EXPECTED_EMBEDDING_DIM) {
            log.error("RAG: Embedding dimension mismatch! Got {} but expected {}. " +
                    "Check gemini embedding model or ALTER TABLE faq_documents ALTER COLUMN embedding TYPE vector({});",
                    queryVector.length, EXPECTED_EMBEDDING_DIM, queryVector.length);
            return List.of();
        }

        // 2. Format vector chuẩn để query PostgreSQL pgvector
        String vectorString = formatVectorForQuery(queryVector);

        // 3. Search DB với threshold để lọc kết quả không liên quan
        log.info("RAG: Searching top {} docs (dim={}) with cosine distance < {}",
                topK, queryVector.length, SIMILARITY_THRESHOLD);
        try {
            List<FaqDocument> results = documentRepository.findTopSimilarDocuments(
                    vectorString, topK, SIMILARITY_THRESHOLD);

            if (results.isEmpty()) {
                log.warn("RAG: No documents within threshold {}. Trying without threshold...", SIMILARITY_THRESHOLD);
                // Fallback: lấy top K không cần threshold để xem khoảng cách thực tế
                List<FaqDocument> all = documentRepository.findTopSimilarDocuments(vectorString, topK, 2.0);
                if (!all.isEmpty()) {
                    log.warn("RAG: Best match without threshold: '{}' (distance quá xa). " +
                            "Xem xét tăng SIMILARITY_THRESHOLD.", all.get(0).getTitle());
                }
            } else {
                log.info("RAG: Found {} relevant doc(s): {}",
                        results.size(),
                        results.stream().map(FaqDocument::getTitle).toList());
            }
            return results;
        } catch (Exception e) {
            log.error("RAG pgvector search failed: {}. Check: 1) CREATE EXTENSION vector; 2) Dimension mismatch.",
                    e.getMessage());
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
