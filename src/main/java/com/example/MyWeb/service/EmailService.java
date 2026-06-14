package com.example.MyWeb.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@myweb.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public void sendHtmlMessage(String to, String subject, String htmlBody) {
        if (mailSender == null) {
            log.warn("[EmailService] JavaMailSender chưa được cấu hình (MOCK MODE). Email sẽ KHÔNG được gửi thật!");
            log.warn("[EmailService] MOCK To: {}", to);
            log.warn("[EmailService] MOCK Subject: {}", subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("[EmailService]  Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("[EmailService] Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    private String getBaseEmailTemplate(String title, String content) {
        return "<!DOCTYPE html>"
                + "<html lang=\"vi\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<style>"
                + "  body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f4f7fa; margin: 0; padding: 0; color: #333; }"
                + "  .email-container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }"
                + "  .header { background-color: #6366f1; color: white; text-align: center; padding: 30px 20px; }"
                + "  .header h1 { margin: 0; font-size: 24px; font-weight: 600; letter-spacing: 1px; }"
                + "  .content { padding: 40px 30px; line-height: 1.6; font-size: 16px; }"
                + "  .content h2 { color: #1f2937; font-size: 20px; margin-top: 0; }"
                + "  .footer { background-color: #f9fafb; border-top: 1px solid #e5e7eb; text-align: center; padding: 20px; font-size: 14px; color: #6b7280; }"
                + "  .btn { display: inline-block; background-color: #6366f1; color: #ffffff !important; text-decoration: none; padding: 12px 24px; border-radius: 6px; font-weight: bold; margin-top: 20px; text-align: center; }"
                + "  .btn:hover { background-color: #4f46e5; }"
                + "  .summary-box { background-color: #f9fafb; border: 1px solid #e5e7eb; border-radius: 6px; padding: 20px; margin: 20px 0; }"
                + "  .summary-item { display: flex; justify-content: space-between; margin-bottom: 10px; border-bottom: 1px dashed #e5e7eb; padding-bottom: 10px; }"
                + "  .summary-item:last-child { border-bottom: none; margin-bottom: 0; padding-bottom: 0; font-weight: bold; font-size: 18px; color: #111827; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "  <div class=\"email-container\">"
                + "    <div class=\"header\">"
                + "      <h1>MEMESHOP</h1>"
                + "    </div>"
                + "    <div class=\"content\">"
                + content
                + "    </div>"
                + "    <div class=\"footer\">"
                + "      <p>Cảm ơn bạn đã mua sắm tại MemeShop!</p>"
                + "      <p>© 2026 MemeShop. All rights reserved.</p>"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }

    public void sendOrderConfirmation(String to, Long orderId, Double amount) {
        String subject = "🎉 Đặt hàng thành công - Đơn hàng #" + orderId;
        String formattedAmount = String.format("%,.0f", amount);

        String content = "<h2>Xin chào!</h2>"
                + "<p>Cảm ơn bạn đã tin tưởng và đặt hàng tại MemeShop. Đơn hàng của bạn đã được ghi nhận trên hệ thống và đang chờ xử lý.</p>"
                + "<div class=\"summary-box\">"
                + "  <div class=\"summary-item\"><span>Mã đơn hàng:</span> <span>#" + orderId + "</span></div>"
                + "  <div class=\"summary-item\"><span>Trạng thái:</span> <span>Chờ xử lý</span></div>"
                + "  <div class=\"summary-item\"><span>Tổng tiền:</span> <span>" + formattedAmount + " VNĐ</span></div>"
                + "</div>"
                + "<p>Bạn có thể theo dõi tiến trình đơn hàng bằng cách nhấn vào nút bên dưới:</p>"
                + "<div style=\"text-align: center;\"><a href=\"" + frontendUrl
                + "/account/orders\" class=\"btn\">Xem đơn hàng của bạn</a></div>";

        sendHtmlMessage(to, subject, getBaseEmailTemplate("Đặt hàng thành công", content));
    }

    public void sendOrderStatusUpdate(String to, Long orderId, String status) {
        String subject = "Cập nhật trạng thái đơn hàng #" + orderId;
        String content = "<h2>Cập nhật đơn hàng #" + orderId + "</h2>"
                + "<p>Đơn hàng của bạn vừa được cập nhật trạng thái mới.</p>"
                + "<div class=\"summary-box\">"
                + "  <div class=\"summary-item\"><span>Trạng thái mới:</span> <span style=\"color: #2563eb; font-weight: bold;\">"
                + status + "</span></div>"
                + "</div>"
                + "<div style=\"text-align: center;\"><a href=\"" + frontendUrl
                + "/account/orders\" class=\"btn\">Kiểm tra ngay</a></div>";

        sendHtmlMessage(to, subject, getBaseEmailTemplate("Cập nhật đơn hàng", content));
    }

    public void sendPaymentConfirmation(String to, Long orderId, Double amount) {
        String subject = "✅ Thanh toán thành công - Đơn hàng #" + orderId;
        String formattedAmount = String.format("%,.0f", amount);

        String content = "<h2>Xin chào!</h2>"
                + "<p>Tuyệt vời! Thanh toán qua VNPay cho đơn hàng <strong>#" + orderId
                + "</strong> của bạn đã được xác nhận thành công.</p>"
                + "<div class=\"summary-box\">"
                + "  <div class=\"summary-item\"><span>Mã đơn hàng:</span> <span>#" + orderId + "</span></div>"
                + "  <div class=\"summary-item\"><span>Phương thức:</span> <span>VNPay</span></div>"
                + "  <div class=\"summary-item\"><span>Đã thanh toán:</span> <span style=\"color: #059669;\">"
                + formattedAmount + " VNĐ</span></div>"
                + "</div>"
                + "<p>Đơn hàng của bạn hiện đang được đóng gói và sẽ sớm được giao. Cảm ơn bạn đã đồng hành cùng MemeShop! 🛍️</p>"
                + "<div style=\"text-align: center;\"><a href=\"" + frontendUrl
                + "/account/orders\" class=\"btn\">Xem chi tiết đơn hàng</a></div>";

        sendHtmlMessage(to, subject, getBaseEmailTemplate("Thanh toán thành công", content));
    }

    public void sendPaymentFailed(String to, Long orderId, String errorCode) {
        String subject = "❌ Thanh toán thất bại - Đơn hàng #" + orderId;

        String content = "<h2>Thanh toán không thành công</h2>"
                + "<p>Rất tiếc, giao dịch thanh toán qua VNPay cho đơn hàng <strong>#" + orderId
                + "</strong> của bạn đã gặp lỗi.</p>"
                + "<div class=\"summary-box\">"
                + "  <div class=\"summary-item\"><span>Mã đơn hàng:</span> <span>#" + orderId + "</span></div>"
                + "  <div class=\"summary-item\"><span>Mã lỗi VNPay:</span> <span style=\"color: #dc2626;\">"
                + errorCode + "</span></div>"
                + "</div>"
                + "<p>Bạn đừng lo lắng, hãy truy cập vào trang đơn hàng và thử thanh toán lại với phương thức khác hoặc thử lại lần nữa nhé.</p>"
                + "<div style=\"text-align: center;\"><a href=\"" + frontendUrl
                + "/account/orders\" class=\"btn\">Thử thanh toán lại</a></div>";

        sendHtmlMessage(to, subject, getBaseEmailTemplate("Thanh toán thất bại", content));
    }
}
