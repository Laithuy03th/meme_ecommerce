package com.example.MyWeb.config;

import com.example.MyWeb.repository.FaqDocumentRepository;
import com.example.MyWeb.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FaqDataSeeder {

    private final LlmService llmService;
    private final JdbcTemplate jdbcTemplate;  // Dùng JdbcTemplate thay @Modifying để tránh transaction proxy issue

    private static final long EXPECTED_COUNT = 6L;
    // model API thực tế trả 3072 dims — phải khớp với cột vector(3072) trong DB
    private static final int EXPECTED_EMBEDDING_DIM = 3072;

    @Bean
    public CommandLineRunner initFaqData(FaqDocumentRepository faqRepository) {
        return args -> {
            try {
                long currentCount = faqRepository.count();

                if (currentCount >= EXPECTED_COUNT) {
                    log.info("RAG FAQ: {} documents already seeded. Skipping.", currentCount);
                    return;
                }

                if (currentCount > 0) {
                    log.info("RAG FAQ: Found {} old documents, clearing before re-seed...", currentCount);
                    faqRepository.deleteAll();
                } else {
                    log.info("RAG FAQ: Starting initial seed of {} policy documents...", EXPECTED_COUNT);
                }

                // 6 policy documents: [title, category, content]
                List<String[]> documents = List.of(
                    new String[]{
                        "Các hình thức thanh toán",
                        "payment",
                        "MemeShop hỗ trợ các hình thức thanh toán: " +
                        "1) Thanh toán khi nhận hàng (COD), áp dụng cho đơn hàng trong nội địa Việt Nam. " +
                        "2) Thanh toán qua VNPay, hỗ trợ thẻ ATM nội địa, thẻ Visa/Mastercard/JCB và QR Code. " +
                        "Khách hàng chọn phương thức thanh toán ở bước checkout trước khi xác nhận đơn hàng."
                    },
                    new String[]{
                        "Quy trình thanh toán và xử lý lỗi thanh toán",
                        "payment",
                        "Quy trình thanh toán tại MemeShop gồm: chọn sản phẩm, thêm vào giỏ hàng, " +
                        "kiểm tra thông tin nhận hàng, chọn phương thức thanh toán, xác nhận đơn hàng và hoàn tất thanh toán. " +
                        "Nếu chọn VNPay, hệ thống sẽ chuyển khách hàng sang cổng thanh toán tương ứng. " +
                        "Nếu bị trừ tiền nhưng đơn hàng chưa được tạo hoặc trạng thái chưa cập nhật, " +
                        "khách hàng nên chụp màn hình giao dịch và liên hệ support để được kiểm tra. " +
                        "Thời gian xử lý hoàn tiền phụ thuộc vào phương thức thanh toán."
                    },
                    new String[]{
                        "Chính sách đổi trả hàng",
                        "return",
                        "Khách hàng có thể yêu cầu đổi trả hàng trong vòng 7 ngày kể từ ngày nhận hàng. " +
                        "Điều kiện đổi trả: sản phẩm còn nguyên vẹn, chưa qua sử dụng, còn nguyên tem nhãn và bao bì gốc. " +
                        "Sản phẩm lỗi do nhà sản xuất, giao sai mẫu, sai kích cỡ hoặc không đúng mô tả sẽ được hỗ trợ đổi trả. " +
                        "Không áp dụng đổi trả với sản phẩm đã qua sử dụng, mất tem nhãn, " +
                        "hư hỏng do khách hàng gây ra hoặc sản phẩm cá nhân hóa theo yêu cầu. " +
                        "Sau khi yêu cầu đổi trả được duyệt, MemeShop sẽ hướng dẫn khách gửi hàng về kho " +
                        "để kiểm tra và xử lý đổi hàng hoặc hoàn tiền."
                    },
                    new String[]{
                        "Chính sách giao hàng",
                        "shipping",
                        "MemeShop giao hàng theo địa chỉ khách hàng cung cấp khi đặt hàng. " +
                        "Phí giao hàng được hiển thị tại bước thanh toán trước khi khách xác nhận đơn. " +
                        "Thời gian giao hàng dự kiến: nội thành Hà Nội và TP.HCM từ 1-2 ngày làm việc; " +
                        "các tỉnh thành khác từ 3-5 ngày làm việc; khu vực xa hơn có thể mất nhiều thời gian hơn. " +
                        "Khi đơn hàng được bàn giao cho đơn vị vận chuyển, khách hàng có thể theo dõi trạng thái giao hàng " +
                        "trong phần đơn hàng."
                    },
                    new String[]{
                        "Chính sách voucher và mã giảm giá",
                        "voucher",
                        "Voucher hoặc mã giảm giá có thể được áp dụng tại bước thanh toán nếu đơn hàng thỏa điều kiện. " +
                        "Các điều kiện thường gặp gồm: giá trị đơn hàng tối thiểu, thời gian hiệu lực, số lượt sử dụng còn lại, " +
                        "nhóm sản phẩm áp dụng hoặc đối tượng khách hàng áp dụng. " +
                        "Mỗi đơn hàng có thể chỉ được dùng một voucher tùy theo quy định của chương trình. " +
                        "Nếu voucher không áp dụng được, khách hàng cần kiểm tra lại điều kiện sử dụng, hạn dùng " +
                        "và sản phẩm trong giỏ hàng."
                    },
                    new String[]{
                        "Chính sách bảo hành",
                        "warranty",
                        "Sản phẩm tại MemeShop được hỗ trợ bảo hành theo điều kiện của cửa hàng hoặc nhà cung cấp. " +
                        "Khi yêu cầu bảo hành, khách hàng cần cung cấp thông tin đơn hàng, hình ảnh hoặc mô tả lỗi sản phẩm. " +
                        "Bảo hành thường áp dụng với lỗi kỹ thuật hoặc lỗi sản xuất. " +
                        "Bảo hành không áp dụng cho hư hỏng do sử dụng sai cách, rơi vỡ, vào nước, tác động vật lý " +
                        "hoặc tự ý sửa chữa. " +
                        "Khách hàng có thể liên hệ support để được hướng dẫn quy trình bảo hành cụ thể."
                    }
                );

                log.info("RAG FAQ: Generating embeddings for {} policy documents...", documents.size());
                int success = 0;

                for (String[] doc : documents) {
                    String title    = doc[0];
                    String category = doc[1];
                    String content  = doc[2];

                    try {
                        String textToEmbed = title + "\n" + category + "\n" + content;
                        float[] embedding = llmService.embed(textToEmbed);

                        if (!isValidEmbedding(embedding)) {
                            log.warn("RAG FAQ: Invalid embedding for '{}', skipping.", title);
                            continue;
                        }

                        // Dùng JdbcTemplate.update() thay vì @Modifying JPA query
                        // để tránh "Executing an update/delete query" (transaction proxy issue)
                        String vectorString = toVectorString(embedding);
                        jdbcTemplate.update(
                            "INSERT INTO faq_documents (category, content, created_at, embedding, title) " +
                            "VALUES (?, ?, NOW(), CAST(? AS vector), ?)",
                            category, content, vectorString, title
                        );

                        success++;
                        log.info("RAG FAQ: [{}/{}] Seeded '{}' ✓", success, documents.size(), title);

                        // Sleep để tránh rate limit Gemini free tier (15 RPM)
                        Thread.sleep(2000);

                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("RAG FAQ: Interrupted during seeding", ie);
                        break;
                    } catch (Exception e) {
                        log.error("RAG FAQ: Failed to seed '{}': {}", title, e.getMessage(), e);
                    }
                }

                log.info("RAG FAQ: Done! Seeded {}/{} policy documents successfully.", success, documents.size());

            } catch (Exception e) {
                log.error("RAG FAQ: Seeding failed. Error: {}", e.getMessage(), e);
            }
        };
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private boolean isValidEmbedding(float[] embedding) {
        if (embedding == null || embedding.length == 0) return false;
        // Kiểm tra đúng dimension trước khi insert vào vector(3072)
        if (embedding.length != EXPECTED_EMBEDDING_DIM) {
            log.error("RAG FAQ: Embedding dimension mismatch! Got {} but expected {}. " +
                      "Check gemini embedding model config.", embedding.length, EXPECTED_EMBEDDING_DIM);
            return false;
        }
        double norm = 0.0;
        for (float v : embedding) norm += v * v;
        return norm > 1e-6;
    }
}