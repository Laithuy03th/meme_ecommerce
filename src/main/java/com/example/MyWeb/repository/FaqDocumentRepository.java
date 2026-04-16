package com.example.MyWeb.repository;

import com.example.MyWeb.model.FaqDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaqDocumentRepository extends JpaRepository<FaqDocument, Long> {

    /**
     * Tìm kiếm top K documents bằng Cosine Similarity (Hỗ trợ bởi pgvector).
     * Toán tử <=> tính khoảng cách cosine giữa 2 vector. Khoảng cách càng nhỏ càng giống nhau.
     * Cần cài đặt extension pgvector trong PostgreSQL: CREATE EXTENSION IF NOT EXISTS vector;
     */
    @Query(value = "SELECT * FROM faq_documents ORDER BY embedding <=> cast(:queryVector as vector) LIMIT :topK", nativeQuery = true)
    List<FaqDocument> findTopSimilarDocuments(@Param("queryVector") String queryVector, @Param("topK") int topK);
}
