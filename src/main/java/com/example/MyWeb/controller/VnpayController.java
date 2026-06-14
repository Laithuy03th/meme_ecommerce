package com.example.MyWeb.controller;

import com.example.MyWeb.model.Order;
import com.example.MyWeb.model.PaymentTransaction;
import com.example.MyWeb.service.EmailService;
import com.example.MyWeb.model.enums.PaymentMethod;
import com.example.MyWeb.model.enums.PaymentStatus;
import com.example.MyWeb.repository.OrderRepository;
import com.example.MyWeb.repository.PaymentTransactionRepository;
import com.example.MyWeb.util.VnpayUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class VnpayController {

    private final Environment env;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // =========================================================================
    // POST /api/v1/payments/vnpay/initiate — Tạo URL thanh toán VNPAY
    // =========================================================================

    @PostMapping("/api/v1/payments/vnpay/initiate")
    public ResponseEntity<?> initiate(@jakarta.validation.Valid @RequestBody InitReq r,
            HttpServletRequest httpRequest) {
        if (r.amount == null || r.amount <= 0)
            return ResponseEntity.badRequest().body(Map.of("message", "amount invalid"));

        String tmn = env.getProperty("VNP_TMNCODE");
        String secret = env.getProperty("VNP_HASHSECRET");
        String payUrl = env.getProperty("VNP_PAY_URL");
        String txnRef = "ORDER_" + r.orderId + "_" + System.currentTimeMillis();

        // Lấy IP thực của client
        String clientIp = getClientIp(httpRequest);

        var now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        var fmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        Map<String, String> v = new HashMap<>();
        v.put("vnp_Version", "2.1.0");
        v.put("vnp_Command", "pay");
        v.put("vnp_TmnCode", tmn);
        long amountVal = (long) (r.amount * 100);
        v.put("vnp_Amount", String.valueOf(amountVal));
        v.put("vnp_CurrCode", "VND");
        v.put("vnp_TxnRef", txnRef);
        v.put("vnp_OrderInfo", r.orderInfo != null ? r.orderInfo : ("Thanh toan don #" + r.orderId));
        v.put("vnp_OrderType", "other");
        v.put("vnp_Locale", r.locale != null ? r.locale : "vn");
        v.put("vnp_ReturnUrl", r.returnUrl);
        if (r.ipnUrl != null && !r.ipnUrl.isBlank()) {
            v.put("vnp_IpnUrl", r.ipnUrl);
        }
        v.put("vnp_IpAddr", clientIp);
        v.put("vnp_CreateDate", now.format(fmt));
        v.put("vnp_ExpireDate", now.plusMinutes(15).format(fmt));

        String sign = VnpayUtils.hmac512(secret, VnpayUtils.toQuery(v));
        String url = payUrl + "?" + VnpayUtils.toQuery(v) + "&vnp_SecureHash=" + sign;

        log.info("[VNPay] Generated payment URL for order={}, txnRef={}, ip={}", r.orderId, txnRef, clientIp);

        Optional<Order> orderOpt = orderRepository.findById(r.orderId);
        if (orderOpt.isPresent()) {
            PaymentTransaction tx = PaymentTransaction.builder()
                    .order(orderOpt.get())
                    .paymentMethod(PaymentMethod.VNPAY)
                    .paymentStatus(PaymentStatus.UNPAID)
                    .transactionId(txnRef) // txnRef = idempotent key cho IPN
                    .paymentUrl(url)
                    .amount(r.amount.doubleValue())
                    .ipAddress(clientIp)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            paymentTransactionRepository.save(tx);
            log.info("[VNPay] Saved PaymentTransaction PENDING for order={}, txnRef={}", r.orderId, txnRef);
        } else {
            log.warn("[VNPay] Order not found: {}. PaymentTransaction not saved.", r.orderId);
        }

        return ResponseEntity.ok(Map.of(
                "orderId", r.orderId,
                "paymentMethod", "VNPAY",
                "paymentUrl", url,
                "txnRef", txnRef));
    }

    // =========================================================================
    // GET /api/v1/payments/vnpay-return — VNPAY redirect sau khi thanh toán
    // =========================================================================

    @GetMapping("/api/v1/payments/vnpay-return")
    public void vnpReturn(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> p = req.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));

        String recv = p.remove("vnp_SecureHash");
        p.remove("vnp_SecureHashType");
        boolean ok = VnpayUtils.hmac512(env.getProperty("VNP_HASHSECRET"), VnpayUtils.toQuery(p))
                .equalsIgnoreCase(recv);
        String code = p.getOrDefault("vnp_ResponseCode", "99");

        // Lấy orderId từ txnRef (format: ORDER_{id}_{time})
        String txnRef = p.get("vnp_TxnRef");
        String orderId = "";
        if (txnRef != null && txnRef.startsWith("ORDER_")) {
            String[] parts = txnRef.split("_");
            if (parts.length >= 2)
                orderId = parts[1];
        }

        String baseUrl = (frontendUrl != null && !frontendUrl.isEmpty()) ? frontendUrl : "http://localhost:3000";
        String redirectUrl = baseUrl + "/payment/result?orderId=" + orderId
                + "&code=" + code
                + "&sig=" + (ok ? "ok" : "bad");

        log.info("[VNPay] Return callback: orderId={}, code={}, sigValid={}", orderId, code, ok);
        resp.sendRedirect(redirectUrl);
    }

    // =========================================================================
    // GET /api/v1/payments/vnpay-ipn — IPN server-to-server (VNPAY → Backend)
    // =========================================================================

    @GetMapping("/api/v1/payments/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpIpn(HttpServletRequest req) {
        Map<String, String> p = req.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));

        if (!p.containsKey("vnp_SecureHash")) {
            log.warn("[VNPay IPN] Missing vnp_SecureHash");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("RspCode", "97", "Message", "Missing vnp_SecureHash"));
        }

        // 1. Verify HMAC signature
        String recv = p.remove("vnp_SecureHash");
        p.remove("vnp_SecureHashType");
        String calc = VnpayUtils.hmac512(env.getProperty("VNP_HASHSECRET"), VnpayUtils.toQuery(p));
        if (!calc.equalsIgnoreCase(recv)) {
            log.warn("[VNPay IPN] Invalid signature");
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid signature"));
        }

        String respCode = p.getOrDefault("vnp_ResponseCode", "99");
        String txnRef = p.get("vnp_TxnRef");
        long amount = Long.parseLong(p.getOrDefault("vnp_Amount", "0")) / 100;

        log.info("[VNPay IPN] Received: txnRef={}, respCode={}, amount={}", txnRef, respCode, amount);

        // 2.Idempotent check — tìm PaymentTransaction theo txnRef
        Optional<PaymentTransaction> txOpt = paymentTransactionRepository.findByTransactionId(txnRef);
        if (txOpt.isEmpty()) {
            log.warn("[VNPay IPN] txnRef not found in DB: {}", txnRef);
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
        }

        PaymentTransaction tx = txOpt.get();

        // 3.Idempotent check — nếu đã xử lý rồi (PAID hoặc FAILED) thì không làm gì
        // thêm
        if (tx.getPaymentStatus() == PaymentStatus.PAID
                || tx.getPaymentStatus() == PaymentStatus.FAILED) {
            log.info("[VNPay IPN] Already processed txnRef={}, status={} — skip", txnRef, tx.getPaymentStatus());
            return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order already confirmed"));
        }

        // 4. Validate amount khớp với DB
        double storedAmount = tx.getAmount() != null ? tx.getAmount() : 0;
        if (Math.abs(storedAmount - amount) > 0.01) {
            log.warn("[VNPay IPN] Amount mismatch: stored={}, received={}", storedAmount, amount);
            return ResponseEntity.ok(Map.of("RspCode", "04", "Message", "Invalid amount"));
        }

        // 5. Cập nhật trạng thái theo kết quả thanh toán
        Order order = tx.getOrder();
        LocalDateTime now = LocalDateTime.now();

        if ("00".equals(respCode)) {
            // Thanh toán thành công
            tx.setPaymentStatus(PaymentStatus.PAID);
            tx.setGatewayResponse(respCode);
            tx.setCompletedAt(now);
            tx.setUpdatedAt(now);
            paymentTransactionRepository.save(tx);

            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            log.info("[VNPay IPN] Payment SUCCESS: orderId={}, txnRef={}", order.getId(), txnRef);

            // Gửi email xác nhận thanh toán thành công
            try {
                String userEmail = order.getUser().getEmail();
                emailService.sendPaymentConfirmation(userEmail, order.getId(), tx.getAmount());
                log.info("[VNPay IPN] Payment confirmation email sent to {}", userEmail);
            } catch (Exception e) {
                log.error("[VNPay IPN] Failed to send payment confirmation email: {}", e.getMessage());
            }
        } else {
            // Thanh toán thất bại hoặc bị hủy
            tx.setPaymentStatus(PaymentStatus.FAILED);
            tx.setGatewayResponse(respCode);
            tx.setErrorMessage("VNPay response code: " + respCode);
            tx.setUpdatedAt(now);
            paymentTransactionRepository.save(tx);

            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            log.info("[VNPay IPN] Payment FAILED: orderId={}, txnRef={}, code={}", order.getId(), txnRef, respCode);

            // Gửi email thông báo thanh toán thất bại
            try {
                String userEmail = order.getUser().getEmail();
                emailService.sendPaymentFailed(userEmail, order.getId(), respCode);
                log.info("[VNPay IPN] Payment failed email sent to {}", userEmail);
            } catch (Exception e) {
                log.error("[VNPay IPN] Failed to send payment failed email: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Lấy IP thực của client, xử lý cả trường hợp qua proxy/load balancer.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    // =========================================================================
    // Request DTOs
    // =========================================================================

    public static class InitReq {
        @jakarta.validation.constraints.NotNull(message = "Order ID is required")
        public Long orderId;

        @jakarta.validation.constraints.NotNull(message = "Amount is required")
        @jakarta.validation.constraints.Min(value = 1000, message = "Amount must be at least 1000 VND")
        public Long amount;

        public String orderInfo;
        public String locale;

        @jakarta.validation.constraints.NotBlank(message = "Return URL is required")
        public String returnUrl;

        public String ipnUrl;
    }
}
