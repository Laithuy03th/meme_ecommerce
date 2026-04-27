package com.example.MyWeb.service;

import com.example.MyWeb.model.FaqDocument;
import com.example.MyWeb.repository.FaqDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service index embedding cho FAQ documents.
 * Chạy một lần sau khi seed dữ liệu FAQ để tạo vector embedding cho pgvector search.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FaqIndexingService {

    private final FaqDocumentRepository faqDocumentRepository;
    private final LlmService llmService;

    /**
     * Index embedding cho tất cả FAQ document chưa có embedding.
     * @return số document đã được index thành công
     */
    @Transactional
    public int indexMissingEmbeddings() {
        List<FaqDocument> docs = faqDocumentRepository.findDocumentsMissingEmbedding();

        if (docs.isEmpty()) {
            log.info("[FaqIndex] All documents already have embeddings.");
            return 0;
        }

        log.info("[FaqIndex] Found {} documents missing embeddings. Starting indexing...", docs.size());
        int indexed = 0;

        for (FaqDocument doc : docs) {
            try {
                // Kết hợp title + content để embedding phong phú hơn
                String textToEmbed = doc.getTitle() + "\n" + doc.getContent();
                float[] embedding = llmService.embed(textToEmbed);

                if (embedding == null || embedding.length == 0) {
                    log.warn("[FaqIndex] Empty embedding for doc id={}, title='{}'. Skipping.",
                            doc.getId(), doc.getTitle());
                    continue;
                }

                // Format float[] thành "[0.123,0.456,...]" cho pgvector
                String vectorString = formatVectorString(embedding);

                // Dùng native update để tránh lỗi Hibernate pgvector type
                faqDocumentRepository.updateEmbeddingById(doc.getId(), vectorString);

                log.info("[FaqIndex] Indexed doc id={}, title='{}'", doc.getId(), doc.getTitle());
                indexed++;

                // Sleep nhỏ để tránh rate limit Gemini Embedding API
                Thread.sleep(500);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("[FaqIndex] Interrupted during indexing", ie);
                break;
            } catch (Exception e) {
                log.error("[FaqIndex] Failed to index doc id={}: {}", doc.getId(), e.getMessage());
            }
        }

        log.info("[FaqIndex] Completed. Indexed {}/{} documents.", indexed, docs.size());
        return indexed;
    }

    private String formatVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
