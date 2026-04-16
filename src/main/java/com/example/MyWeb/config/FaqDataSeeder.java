package com.example.MyWeb.config;

import com.example.MyWeb.model.FaqDocument;
import com.example.MyWeb.repository.FaqDocumentRepository;
import com.example.MyWeb.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FaqDataSeeder {

    private final LlmService llmService;

    @Bean
    public CommandLineRunner initFaqData(FaqDocumentRepository faqRepository) {
        return args -> {
            try {
                if (faqRepository.count() > 0) {
                    log.info("RAG FAQ documents already seeded. Skipping.");
                    return;
                }

                log.info("Initializing RAG FAQ documents...");

                List<FaqDocument> documents = Arrays.asList(
                    FaqDocument.builder()
                        .title("Chính sách giao hàng")
                        .category("shipping")
                        .content("Miễn phí giao hàng cho đơn từ 500.000đ. Nội thành nhận hàng trong 1-2 ngày, ngoại thành từ 3-5 ngày làm việc. Phí ship tiêu chuẩn dao động từ 30.000đ đến 50.000đ tùy khu vực.")
                        .build(),
                    FaqDocument.builder()
                        .title("Đổi trả hàng hóa")
                        .category("return")
                        .content("Bạn có thể đổi trả hàng trong vòng 7 ngày kể từ ngày nhận. Điều kiện: Sản phẩm phải còn nguyên vẹn, chưa qua sử dụng và còn nguyên seal. Hoàn tiền sẽ được xử lý trong 5-7 ngày làm việc. Không áp dụng đổi trả cho các sản phẩm sale trên 50%.")
                        .build(),
                    FaqDocument.builder()
                        .title("Chính sách bảo hành")
                        .category("warranty")
                        .content("Hàng điện tử được bảo hành chính hãng từ 12-24 tháng. Các sản phẩm thời trang được hỗ trợ bảo hành trong 30 ngày đối với lỗi do nhà sản xuất. Đối với phụ kiện, thời gian bảo hành từ 3-6 tháng.")
                        .build(),
                    FaqDocument.builder()
                        .title("Phương thức thanh toán")
                        .category("payment")
                        .content("Cửa hàng hỗ trợ thanh toán khi nhận hàng (COD) và thanh toán qua VNPay (bao gồm thẻ ATM nội địa, Visa, Mastercard và quét mã QR Code). Quá trình thanh toán trực tuyến được bảo mật qua SSL an toàn.")
                        .build(),
                    FaqDocument.builder()
                        .title("Sử dụng Voucher")
                        .category("voucher")
                        .content("Voucher giảm giá có thể được áp dụng trực tiếp tại bước thanh toán ở giỏ hàng. Xin lưu ý mỗi đơn hàng chỉ được sử dụng tối đa 1 voucher. Khách hàng vui lòng chú ý điều kiện và thời hạn sử dụng được ghi chú cụ thể trên nội dung của mỗi tài khoản voucher.")
                        .build()
                );

                for (FaqDocument doc : documents) {
                    // Generate vector length 768 from Gemini
                    float[] embedding = llmService.embed(doc.getContent());
                    doc.setEmbedding(embedding);
                    faqRepository.save(doc);
                    
                    // Simple rate limit to prevent Gemini strict limits on free tier
                    Thread.sleep(2000); 
                }

                log.info("Seeded {} FAQ documents with embeddings successfully!", documents.size());

            } catch (Exception e) {
                log.error("Failed to seed FAQ data. Ensure pgvector extension is installed! {}", e.getMessage());
            }
        };
    }
}
