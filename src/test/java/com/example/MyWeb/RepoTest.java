package com.example.MyWeb;

import com.example.MyWeb.model.FaqDocument;
import com.example.MyWeb.repository.FaqDocumentRepository;
import com.example.MyWeb.service.LlmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@SpringBootTest
public class RepoTest {

    @Autowired
    private TestFaqRepo testRepo;

    @Autowired
    private LlmService llmService;

    @Test
    public void testRagSearch() {
        try {
            System.out.println("=== TESTING RAG SEARCH ===");
            float[] queryVector = llmService.embed("chính sách thanh toán");
            
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < queryVector.length; i++) {
                sb.append(queryVector[i]);
                if (i < queryVector.length - 1) sb.append(",");
            }
            sb.append("]");
            String vectorString = sb.toString();

            System.out.println("Vector generated, length: " + queryVector.length);
            
            List<FaqDocument> results = testRepo.findTopSimilarDocumentsWithoutEmbedding(vectorString, 3, 1.2);
            System.out.println("Found results: " + results.size());
            for (FaqDocument doc : results) {
                System.out.println("Result: " + doc.getTitle());
            }
        } catch (Exception e) {
            System.out.println("RAG_EXCEPTION_CAUGHT");
            e.printStackTrace();
        }
    }
}

@Repository
interface TestFaqRepo extends org.springframework.data.jpa.repository.JpaRepository<FaqDocument, Long> {
    @Query(value = "SELECT id, category, content, created_at, NULL\\:\\:vector as embedding, title FROM faq_documents " +
                   "WHERE (embedding <=> cast(:queryVector as vector)) < :threshold " +
                   "ORDER BY embedding <=> cast(:queryVector as vector) " +
                   "LIMIT :topK", nativeQuery = true)
    List<FaqDocument> findTopSimilarDocumentsWithoutEmbedding(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK,
            @Param("threshold") double threshold
    );
}
