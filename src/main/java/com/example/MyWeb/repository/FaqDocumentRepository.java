package com.example.MyWeb.repository;

import com.example.MyWeb.model.FaqDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaqDocumentRepository extends JpaRepository<FaqDocument, Long> {

    /**
     * Tìm kiếm top K documents bằng Cosine Distance (pgvector).
     * Toán tử <=> tính khoảng cách cosine (gần 0 = giống nhau, gần 2 = hoàn toàn khác nhau).
     * Cần: CREATE EXTENSION IF NOT EXISTS vector; trong PostgreSQL.
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

    /**
     * Tìm các document chưa có embedding.
     */
    @Query(value = "SELECT * FROM faq_documents WHERE embedding IS NULL", nativeQuery = true)
    List<FaqDocument> findDocumentsMissingEmbedding();

    /**
     * Native INSERT để bypass lỗi Hibernate bind float[] -> bytea.
     * Embedding phải được format thành string "[0.1,0.2,...]" rồi CAST sang vector.
     */
    @Modifying
    @Query(value = "INSERT INTO faq_documents (category, content, created_at, embedding, title) " +
                   "VALUES (:category, :content, NOW(), CAST(:embedding AS vector), :title)",
           nativeQuery = true)
    void insertWithEmbedding(
            @Param("category") String category,
            @Param("content") String content,
            @Param("embedding") String embedding,
            @Param("title") String title
    );

    /**
     * Cập nhật embedding cho một document bằng native query.
     */
    @Modifying
    @Query(value = "UPDATE faq_documents SET embedding = CAST(:embedding AS vector) WHERE id = :id",
           nativeQuery = true)
    void updateEmbeddingById(@Param("id") Long id, @Param("embedding") String embedding);
}
