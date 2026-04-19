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

/**
 * Seed dữ liệu FAQ cho RAG (Retrieval-Augmented Generation).
 * Mỗi document sẽ được embed bằng Gemini và lưu vector vào PostgreSQL pgvector.
 *
 * Nếu số lượng document hiện tại < EXPECTED_COUNT, sẽ xóa toàn bộ và seed lại từ đầu.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class FaqDataSeeder {

    private final LlmService llmService;

    // Tăng số này mỗi khi thêm document mới để trigger re-seed
    private static final long EXPECTED_COUNT = 15L;

    @Bean
    public CommandLineRunner initFaqData(FaqDocumentRepository faqRepository) {
        return args -> {
            try {
                long currentCount = faqRepository.count();
                if (currentCount >= EXPECTED_COUNT) {
                    log.info("RAG FAQ: {} documents already seeded. Skipping.", currentCount);
                    return;
                }

                // Xóa dữ liệu cũ và seed lại toàn bộ để đảm bảo nhất quán
                if (currentCount > 0) {
                    log.info("RAG FAQ: Found {} old documents, clearing and re-seeding {} new ones...",
                            currentCount, EXPECTED_COUNT);
                    faqRepository.deleteAll();
                } else {
                    log.info("RAG FAQ: Starting initial seed of {} documents...", EXPECTED_COUNT);
                }

                List<FaqDocument> documents = Arrays.asList(

                    // ================================================================
                    // 1. GIAO HÀNG (Shipping)
                    // ================================================================
                    FaqDocument.builder()
                        .title("Chính sách giao hàng")
                        .category("shipping")
                        .content("MemeShop miễn phí giao hàng toàn quốc cho đơn hàng từ 500.000đ trở lên. " +
                                 "Đối với đơn dưới 500.000đ, phí vận chuyển dao động từ 25.000đ đến 50.000đ tùy khu vực. " +
                                 "Thời gian giao hàng: nội thành Hà Nội và TP.HCM từ 1-2 ngày làm việc; " +
                                 "các tỉnh thành khác từ 3-5 ngày làm việc; vùng sâu vùng xa có thể lên đến 7 ngày. " +
                                 "Chúng tôi hợp tác với các đơn vị vận chuyển uy tín: GHN, GHTK, Viettel Post.")
                        .build(),

                    // ================================================================
                    // 2. GIAO HÀNG NHANH / HỎA TỐC
                    // ================================================================
                    FaqDocument.builder()
                        .title("Giao hàng nhanh và hỏa tốc")
                        .category("shipping")
                        .content("MemeShop hỗ trợ dịch vụ giao hàng nhanh (same-day) tại nội thành Hà Nội và TP.HCM " +
                                 "với phụ phí 30.000đ - 50.000đ. Đơn hàng đặt trước 12:00 trưa sẽ được giao trong ngày. " +
                                 "Dịch vụ giao hàng hỏa tốc (2-3 giờ) áp dụng cho một số khu vực trung tâm thành phố lớn, " +
                                 "phụ phí 70.000đ. Để sử dụng, chọn 'Giao hàng nhanh' tại bước thanh toán khi đặt đơn.")
                        .build(),

                    // ================================================================
                    // 3. ĐỔI TRẢ HÀNG HÓA
                    // ================================================================
                    FaqDocument.builder()
                        .title("Chính sách đổi trả hàng hóa")
                        .category("return")
                        .content("Khách hàng có quyền đổi trả hàng trong vòng 7 ngày kể từ ngày nhận hàng. " +
                                 "Điều kiện đổi trả: Sản phẩm còn nguyên vẹn, chưa qua sử dụng, còn nguyên tem nhãn và bao bì gốc. " +
                                 "Sản phẩm lỗi do nhà sản xuất hoặc giao sai hàng sẽ được đổi miễn phí 100%. " +
                                 "Không áp dụng đổi trả với sản phẩm đang trong chương trình khuyến mãi giảm trên 50%, " +
                                 "đồ lót, sản phẩm cá nhân hóa (in tên, hình theo yêu cầu). " +
                                 "Hoàn tiền sẽ được xử lý trong 5-7 ngày làm việc sau khi nhận được hàng trả.")
                        .build(),

                    // ================================================================
                    // 4. QUY TRÌNH ĐỔI TRẢ
                    // ================================================================
                    FaqDocument.builder()
                        .title("Quy trình thực hiện đổi trả hàng")
                        .category("return")
                        .content("Để đổi trả hàng tại MemeShop, khách hàng thực hiện theo các bước sau: " +
                                 "Bước 1 - Đăng nhập tài khoản, vào mục 'Đơn hàng của tôi', chọn đơn cần đổi trả. " +
                                 "Bước 2 - Nhấn nút 'Yêu cầu đổi/trả', chọn lý do và mô tả vấn đề, đính kèm ảnh chụp sản phẩm lỗi nếu có. " +
                                 "Bước 3 - Đội ngũ hỗ trợ sẽ xem xét và phản hồi trong vòng 24 giờ làm việc. " +
                                 "Bước 4 - Sau khi được duyệt, khách hàng gửi hàng về kho theo địa chỉ được cung cấp. " +
                                 "Bước 5 - MemeShop nhận hàng, kiểm tra và xử lý đổi hàng mới hoặc hoàn tiền. " +
                                 "Phí vận chuyển hoàn hàng do MemeShop chi trả nếu lỗi từ phía cửa hàng.")
                        .build(),

                    // ================================================================
                    // 5. BẢO HÀNH
                    // ================================================================
                    FaqDocument.builder()
                        .title("Chính sách bảo hành sản phẩm")
                        .category("warranty")
                        .content("Chính sách bảo hành tại MemeShop theo từng danh mục: " +
                                 "Điện tử - công nghệ (laptop, điện thoại, tai nghe...): bảo hành chính hãng 12-24 tháng tùy sản phẩm. " +
                                 "Thời trang (quần áo, giày dép): bảo hành 30 ngày với lỗi do nhà sản xuất (đường may, khóa kéo, nút bị hỏng...). " +
                                 "Phụ kiện (ốp lưng, cáp sạc, túi...): bảo hành 3-6 tháng. " +
                                 "Đồ gia dụng: bảo hành 6-12 tháng. " +
                                 "Bảo hành không áp dụng khi sản phẩm bị hư hỏng do tác động vật lý, ngập nước, hoặc sử dụng sai cách. " +
                                 "Để kích hoạt bảo hành, khách hàng giữ lại hóa đơn hoặc email xác nhận đơn hàng.")
                        .build(),

                    // ================================================================
                    // 6. THANH TOÁN
                    // ================================================================
                    FaqDocument.builder()
                        .title("Phương thức thanh toán")
                        .category("payment")
                        .content("MemeShop hỗ trợ đa dạng phương thức thanh toán: " +
                                 "1) Thanh toán khi nhận hàng (COD): áp dụng cho đơn hàng trong nội địa Việt Nam, " +
                                 "không hỗ trợ COD cho đơn giá trị từ 10 triệu đồng trở lên. " +
                                 "2) VNPay: bao gồm thẻ ATM nội địa, thẻ Visa/Mastercard/JCB, thanh toán qua QR Code. " +
                                 "Tất cả giao dịch trực tuyến được bảo mật qua chuẩn SSL 256-bit. " +
                                 "3) Chuyển khoản ngân hàng: thanh toán thủ công qua số tài khoản được cung cấp, " +
                                 "cần ghi rõ mã đơn hàng trong nội dung chuyển khoản. " +
                                 "Đơn hàng sẽ được xử lý sau 15-30 phút kể từ khi thanh toán thành công.")
                        .build(),

                    // ================================================================
                    // 7. LỖI THANH TOÁN
                    // ================================================================
                    FaqDocument.builder()
                        .title("Xử lý lỗi thanh toán và hoàn tiền")
                        .category("payment")
                        .content("Nếu bạn gặp lỗi trong quá trình thanh toán tại MemeShop: " +
                                 "Trường hợp bị trừ tiền nhưng đơn hàng chưa được tạo: Vui lòng chụp màn hình lịch sử giao dịch " +
                                 "và liên hệ ngay với MemeShop qua email support@memeshop.vn hoặc hotline 1900-xxxx. " +
                                 "Thời gian xử lý hoàn tiền: Thẻ tín dụng/ghi nợ từ 3-5 ngày làm việc; " +
                                 "VNPay/ví điện tử từ 1-3 ngày làm việc; chuyển khoản ngân hàng trong 1 ngày làm việc. " +
                                 "Lưu ý: MemeShop KHÔNG yêu cầu khách hàng cung cấp OTP hoặc mật khẩu tài khoản ngân hàng qua bất kỳ kênh nào.")
                        .build(),

                    // ================================================================
                    // 8. VOUCHER / MÃ GIẢM GIÁ
                    // ================================================================
                    FaqDocument.builder()
                        .title("Hướng dẫn sử dụng voucher và mã giảm giá")
                        .category("voucher")
                        .content("Mã giảm giá (voucher) tại MemeShop được áp dụng trực tiếp tại bước thanh toán trong giỏ hàng. " +
                                 "Cách dùng: Nhập mã vào ô 'Mã giảm giá' và nhấn 'Áp dụng'. " +
                                 "Quy định sử dụng: Mỗi đơn hàng chỉ được sử dụng tối đa 1 voucher. " +
                                 "Voucher có thể có điều kiện: giá trị đơn tối thiểu, áp dụng cho danh mục sản phẩm cụ thể, " +
                                 "hoặc chỉ dành cho khách hàng mới/thành viên VIP. " +
                                 "Voucher có thời hạn sử dụng, vui lòng kiểm tra hạn dùng trước khi thanh toán. " +
                                 "Nếu voucher không áp dụng được, có thể do: đơn chưa đủ điều kiện, đã hết lượt sử dụng, " +
                                 "hoặc sản phẩm trong giỏ không thuộc danh mục áp dụng.")
                        .build(),

                    // ================================================================
                    // 9. TÀI KHOẢN - ĐĂNG KÝ / ĐĂNG NHẬP
                    // ================================================================
                    FaqDocument.builder()
                        .title("Đăng ký và đăng nhập tài khoản MemeShop")
                        .category("account")
                        .content("Để tạo tài khoản MemeShop: Nhấn 'Đăng ký' tại góc phải trên trang chủ, " +
                                 "điền email và mật khẩu (tối thiểu 8 ký tự, gồm chữ và số), " +
                                 "xác nhận email qua link gửi về hộp thư (kiểm tra cả mục Spam). " +
                                 "Đăng nhập bằng email và mật khẩu đã đăng ký, hoặc đăng nhập nhanh qua Google. " +
                                 "Lợi ích khi có tài khoản: theo dõi đơn hàng, lưu địa chỉ giao hàng, " +
                                 "nhận voucher độc quyền, tích lũy điểm thưởng và xem lịch sử mua hàng.")
                        .build(),

                    // ================================================================
                    // 10. QUÊN MẬT KHẨU
                    // ================================================================
                    FaqDocument.builder()
                        .title("Khôi phục mật khẩu và tài khoản bị khóa")
                        .category("account")
                        .content("Nếu quên mật khẩu tài khoản MemeShop: " +
                                 "Nhấn 'Quên mật khẩu?' tại trang đăng nhập, nhập email đã đăng ký. " +
                                 "Hệ thống sẽ gửi link đặt lại mật khẩu về email trong vòng 5 phút (kiểm tra cả thư mục Spam). " +
                                 "Link có hiệu lực trong 1 giờ. " +
                                 "Nếu không nhận được email hoặc tài khoản bị khóa do nhập sai mật khẩu nhiều lần, " +
                                 "vui lòng liên hệ hỗ trợ qua email support@memeshop.vn với tiêu đề 'Khóa tài khoản - [email của bạn]'.")
                        .build(),

                    // ================================================================
                    // 11. THEO DÕI ĐƠN HÀNG
                    // ================================================================
                    FaqDocument.builder()
                        .title("Theo dõi và tra cứu đơn hàng")
                        .category("order")
                        .content("Để theo dõi đơn hàng tại MemeShop sau khi đặt thành công: " +
                                 "Đăng nhập tài khoản → vào mục 'Đơn hàng của tôi' để xem toàn bộ lịch sử. " +
                                 "Các trạng thái đơn hàng: PENDING (chờ xác nhận) → CONFIRMED (đã xác nhận) → " +
                                 "SHIPPED (đang giao hàng) → DELIVERED (đã giao thành công) → CANCELED (đã hủy). " +
                                 "Khi đơn được giao cho đơn vị vận chuyển, bạn sẽ nhận email chứa mã vận đơn (tracking code) " +
                                 "để tra cứu trực tiếp trên website của GHN, GHTK hoặc Viettel Post.")
                        .build(),

                    // ================================================================
                    // 12. HỦY ĐƠN HÀNG
                    // ================================================================
                    FaqDocument.builder()
                        .title("Hủy đơn hàng")
                        .category("order")
                        .content("Khách hàng có thể hủy đơn hàng tại MemeShop khi đơn đang ở trạng thái PENDING (chờ xác nhận). " +
                                 "Cách hủy: Vào 'Đơn hàng của tôi' → chọn đơn cần hủy → nhấn 'Hủy đơn' và chọn lý do. " +
                                 "Đơn đã ở trạng thái CONFIRMED hoặc SHIPPED không thể hủy trực tiếp, " +
                                 "cần liên hệ hỗ trợ khẩn cấp qua hotline 1900-xxxx trong giờ hành chính. " +
                                 "Nếu đã thanh toán trước, tiền sẽ được hoàn lại theo phương thức thanh toán ban đầu " +
                                 "trong 3-7 ngày làm việc.")
                        .build(),

                    // ================================================================
                    // 13. LIÊN HỆ HỖ TRỢ
                    // ================================================================
                    FaqDocument.builder()
                        .title("Liên hệ và hỗ trợ khách hàng MemeShop")
                        .category("support")
                        .content("MemeShop hỗ trợ khách hàng qua các kênh sau: " +
                                 "Email: support@memeshop.vn (phản hồi trong 24 giờ làm việc). " +
                                 "Hotline: 1900-xxxx (Thứ 2 đến Thứ 6, 8:00 - 20:00; Thứ 7 - Chủ nhật, 9:00 - 17:00). " +
                                 "Chat trực tiếp: qua chatbot AI ngay trên website (24/7 cho câu hỏi thông thường). " +
                                 "Fanpage Facebook: /MemeShopVN. " +
                                 "Địa chỉ văn phòng: Hà Nội & TP.HCM (chỉ tiếp nhận khiếu nại, không bán lẻ trực tiếp). " +
                                 "Để được hỗ trợ nhanh nhất, vui lòng cung cấp mã đơn hàng khi liên hệ.")
                        .build(),

                    // ================================================================
                    // 14. KHIẾU NẠI SẢN PHẨM LỖI
                    // ================================================================
                    FaqDocument.builder()
                        .title("Khiếu nại sản phẩm lỗi và hàng giả")
                        .category("support")
                        .content("Nếu nhận được sản phẩm bị lỗi hoặc nghi ngờ hàng giả/hàng nhái tại MemeShop: " +
                                 "Chụp ảnh hoặc quay video rõ ràng về tình trạng sản phẩm ngay khi nhận hàng. " +
                                 "Báo ngay trong vòng 48 giờ kể từ khi nhận hàng qua: mục 'Yêu cầu đổi/trả' trong đơn hàng " +
                                 "hoặc email support@memeshop.vn với tiêu đề 'Khiếu nại sản phẩm - Mã đơn XXXXX'. " +
                                 "MemeShop cam kết đổi mới 100% hoặc hoàn tiền toàn bộ, bao gồm cả phí vận chuyển, " +
                                 "đối với sản phẩm xác nhận bị lỗi hoặc không đúng mô tả. " +
                                 "Tất cả sản phẩm trên MemeShop đều là hàng chính hãng có tem chống giả.")
                        .build(),

                    // ================================================================
                    // 15. SẢN PHẨM ĐẶC THÙ MEMESHOP (Meme / In ấn theo yêu cầu)
                    // ================================================================
                    FaqDocument.builder()
                        .title("Đặc thù sản phẩm meme và in theo yêu cầu")
                        .category("product")
                        .content("MemeShop là cửa hàng thương mại điện tử chuyên bán sản phẩm văn hóa meme Internet " +
                                 "và sản phẩm in theo yêu cầu (print-on-demand): áo thun in hình meme, poster, cốc, " +
                                 "sticker, ốp điện thoại, tote bag với họa tiết meme độc đáo. " +
                                 "Sản phẩm in theo yêu cầu (custom): khách hàng gửi thiết kế, MemeShop in và giao. " +
                                 "Thời gian xử lý đơn custom từ 3-5 ngày làm việc trước khi giao (không bao gồm thời gian ship). " +
                                 "Lưu ý: Sản phẩm custom/in theo yêu cầu KHÔNG áp dụng chính sách đổi trả thông thường, " +
                                 "chỉ hỗ trợ đổi trả nếu in sai nội dung so với yêu cầu ban đầu của khách.")
                        .build()
                );

                log.info("RAG FAQ: Generating embeddings and saving {} documents...", documents.size());
                int success = 0;
                for (FaqDocument doc : documents) {
                    try {
                        float[] embedding = llmService.embed(doc.getContent());
                        if (embedding != null && embedding.length > 0 && embedding[0] != 0f) {
                            doc.setEmbedding(embedding);
                            faqRepository.save(doc);
                            success++;
                            log.info("RAG FAQ: [{}/{}] Seeded '{}' ✓", success, documents.size(), doc.getTitle());
                        } else {
                            log.warn("RAG FAQ: Embedding failed for '{}', skipping.", doc.getTitle());
                        }
                        // Rate limit cho Gemini free tier (15 RPM)
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        log.error("RAG FAQ: Failed to seed document '{}': {}", doc.getTitle(), e.getMessage());
                    }
                }

                log.info("RAG FAQ: Done! Seeded {}/{} documents successfully.", success, documents.size());

            } catch (Exception e) {
                log.error("RAG FAQ: Seeding failed. Ensure pgvector extension is installed! Error: {}", e.getMessage());
            }
        };
    }
}
