package com.example.MyWeb.repository;

import com.example.MyWeb.model.FaqDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaqDocumentRepository extends JpaRepository<FaqDocument, Long> {

    /**
     * Tìm kiếm top K documents bằng Cosine Distance (Hỗ trợ bởi pgvector).
     * Toán tử <=> tính khoảng cách cosine giữa 2 vector (gần 0 = giống nhau, gần 2 = hoàn toàn khác nhau).
     * threshold: Chỉ lấy document có khoảng cách < threshold (ví dụ 0.65 là khá liên quan).
     * Cần cài đặt extension pgvector trong PostgreSQL: CREATE EXTENSION IF NOT EXISTS vector;
     */
    @Query(value = "SELECT * FROM faq_documents " +
                   "WHERE (embedding <=> cast(:queryVector as vector)) < :threshold " +
                   "ORDER BY embedding <=> cast(:queryVector as vector) " +
                   "LIMIT :topK", nativeQuery = true)
    List<FaqDocument> findTopSimilarDocuments(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK,
            @Param("threshold") double threshold
    );
}
