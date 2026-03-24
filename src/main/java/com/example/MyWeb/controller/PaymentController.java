package com.example.MyWeb.controller;

import com.example.MyWeb.dto.payment.PaymentCallbackRequest;
import com.example.MyWeb.dto.payment.PaymentRequest;
import com.example.MyWeb.dto.payment.PaymentResponse;
import com.example.MyWeb.security.CustomUserDetails;
import com.example.MyWeb.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            return null;
        }
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        return principal.getId();
    }

    /**
     * Extract client IP address from HTTP request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For may contain multiple IPs, get the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * POST /api/v1/payments/initiate
     * Khởi tạo thanh toán cho một đơn hàng
     */
    @PostMapping("/initiate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId();
        String clientIp = getClientIp(httpRequest);

        log.info("Payment initiation request from user: {}, IP: {}, order: {}",
                userId, clientIp, request.getOrderId());

        PaymentResponse response = paymentService.initiatePayment(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/payments/callback/{orderId}
     * Nhận callback từ payment gateway (VNPay, Momo, Stripe)
     * Endpoint này có thể được gọi từ payment gateway nên không cần authentication
     */
    @PostMapping("/callback/{orderId}")
    public ResponseEntity<PaymentResponse> handlePaymentCallback(
            @PathVariable Long orderId,
            @RequestBody PaymentCallbackRequest callbackData,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);

        log.info("Payment callback received for order: {}, status: {}, IP: {}",
                orderId, callbackData.getStatus(), clientIp);

        PaymentResponse response = paymentService.handlePaymentCallback(orderId, callbackData);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/payments/vnpay-callback
     * Xử lý callback từ VNPay (Redirect về FE hoặc IPN)
     * VNPay trả về dữ liệu qua Query Params
     */
    @GetMapping("/vnpay-callback")
    public ResponseEntity<PaymentResponse> handleVnPayCallback(
            @RequestParam java.util.Map<String, String> requestParams,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        log.info("VNPay callback received from IP: {}, params: {}", clientIp, requestParams);

        PaymentResponse response = paymentService.handleVnPayCallback(requestParams);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/payments/status/{orderId}
     * Kiểm tra trạng thái thanh toán của đơn hàng
     */
    @GetMapping("/status/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> checkPaymentStatus(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();

        log.info("Payment status check from user: {}, order: {}", userId, orderId);

        PaymentResponse response = paymentService.checkPaymentStatus(userId, orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/payments/refund/{orderId}
     * Xử lý hoàn tiền cho đơn hàng
     */
    @PostMapping("/refund/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> processRefund(
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason) {
        Long userId = getCurrentUserId();

        log.info("Refund request from user: {}, order: {}, reason: {}", userId, orderId, reason);

        PaymentResponse response = paymentService.processRefund(userId, orderId, reason);
        return ResponseEntity.ok(response);
    }
}
