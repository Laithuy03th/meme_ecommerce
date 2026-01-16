package com.example.MyWeb.controller;

import com.example.MyWeb.util.VnpayUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class VnpayController {

    private final Environment env;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendUrl;

    @PostMapping("/api/v1/payments/vnpay/initiate")
    public ResponseEntity<?> initiate(@jakarta.validation.Valid @RequestBody InitReq r) {
        if (r.amount == null || r.amount <= 0)
            return ResponseEntity.badRequest().body(Map.of("message", "amount invalid"));
        String tmn = env.getProperty("VNP_TMNCODE"), secret = env.getProperty("VNP_HASHSECRET");
        String payUrl = env.getProperty("VNP_PAY_URL");
        String txnRef = "ORDER_" + r.orderId + "_" + System.currentTimeMillis();

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
        v.put("vnp_IpnUrl", r.ipnUrl); // HTTPS từ ngrok
        v.put("vnp_IpAddr", "127.0.0.1");
        v.put("vnp_CreateDate", now.format(fmt));
        v.put("vnp_ExpireDate", now.plusMinutes(15).format(fmt));

        String sign = VnpayUtils.hmac512(secret, VnpayUtils.toQuery(v));
        String url = payUrl + "?" + VnpayUtils.toQuery(v) + "&vnp_SecureHash=" + sign;

        System.out.println("---------- VNPAY GENERATED URL ----------");
        System.out.println(url);
        System.out.println("-----------------------------------------");

        // TODO: lưu PaymentAttempt{orderId, txnRef, amount, status=PENDING}
        return ResponseEntity
                .ok(Map.of("orderId", r.orderId, "paymentMethod", "VNPAY", "paymentUrl", url, "txnRef", txnRef));
    }

    @GetMapping("/api/v1/payments/vnpay-return")
    public void vnpReturn(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> p = req.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
        String recv = p.remove("vnp_SecureHash");
        p.remove("vnp_SecureHashType");
        boolean ok = VnpayUtils.hmac512(env.getProperty("VNP_HASHSECRET"), VnpayUtils.toQuery(p))
                .equalsIgnoreCase(recv);
        String code = p.getOrDefault("vnp_ResponseCode", "99");

        // Cố gắng lấy orderId từ txnRef (format: ORDER_{id}_{time})
        String txnRef = p.get("vnp_TxnRef");
        String orderId = "";
        if (txnRef != null && txnRef.startsWith("ORDER_")) {
            String[] parts = txnRef.split("_");
            if (parts.length >= 2)
                orderId = parts[1];
        }

        // Redirect về Frontend (NextJS port 3000)
        String baseUrl = (frontendUrl != null && !frontendUrl.isEmpty()) ? frontendUrl : "http://localhost:3000";
        String redirectUrl = baseUrl + "/payment/result?orderId=" + orderId + "&code=" + code + "&sig="
                + (ok ? "ok" : "bad");
        resp.sendRedirect(redirectUrl);
    }

    @GetMapping("/api/v1/payments/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpIpn(HttpServletRequest req) {
        Map<String, String> p = req.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
        if (!p.containsKey("vnp_SecureHash"))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("RspCode", "97", "Message", "Missing vnp_SecureHash"));

        String recv = p.remove("vnp_SecureHash");
        p.remove("vnp_SecureHashType");
        String calc = VnpayUtils.hmac512(env.getProperty("VNP_HASHSECRET"), VnpayUtils.toQuery(p));
        if (!calc.equalsIgnoreCase(recv))
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid signature"));

        String respCode = p.getOrDefault("vnp_ResponseCode", "99");
        long amount = Long.parseLong(p.get("vnp_Amount")) / 100;
        String txnRef = p.get("vnp_TxnRef");
        // TODO: tra DB: map txnRef -> order, so khớp amount, idempotent
        boolean already = false; // ví dụ

        if (already)
            return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order already confirmed"));
        if ("00".equals(respCode)) {
            // mark PAID
        } else {
            // mark FAILED/CANCELED theo mã
        }
        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }

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
