package com.example.MyWeb.service.impl;

import com.example.MyWeb.config.VNPayConfig;
import com.example.MyWeb.dto.payment.PaymentCallbackRequest;
import com.example.MyWeb.dto.payment.PaymentRequest;
import com.example.MyWeb.dto.payment.PaymentResponse;
import com.example.MyWeb.exception.PaymentException;
import com.example.MyWeb.model.Order;
import com.example.MyWeb.model.PaymentTransaction;
import com.example.MyWeb.model.enums.OrderStatus;
import com.example.MyWeb.model.enums.PaymentMethod;
import com.example.MyWeb.model.enums.PaymentStatus;
import com.example.MyWeb.repository.OrderRepository;
import com.example.MyWeb.repository.PaymentTransactionRepository;
import com.example.MyWeb.service.PaymentService;
import com.example.MyWeb.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final VNPayConfig vnPayConfig;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(Long userId, PaymentRequest request) {
        log.info("Initiating payment for user: {}, order: {}", userId, request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new PaymentException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new PaymentException("Unauthorized access to order");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new PaymentException("Order already paid");
        }

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new PaymentException("Order is cancelled");
        }

        PaymentMethod paymentMethod = order.getPaymentMethod();
        String paymentUrl = null;

        // Create payment transaction record
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.UNPAID)
                .amount(order.getTotalAmount())
                .createdAt(LocalDateTime.now())
                .build();

        try {
            switch (paymentMethod) {
                case COD:
                    log.info("COD payment for order: {}", order.getId());
                    transaction.setPaymentStatus(PaymentStatus.UNPAID);
                    paymentTransactionRepository.save(transaction);

                    return PaymentResponse.builder()
                            .orderId(order.getId())
                            .paymentMethod(paymentMethod.name())
                            .paymentStatus(PaymentStatus.UNPAID.name())
                            .amount(order.getTotalAmount())
                            .message("Order will be paid on delivery (COD)")
                            .build();

                case VNPAY:
                    log.info("Generating VNPay payment URL for order: {}", order.getId());
                    paymentUrl = generateVNPayUrl(order, request.getReturnUrl());
                    break;

                default:
                    throw new PaymentException("Unsupported payment method: " + paymentMethod);
            }

            // Save transaction with payment URL
            transaction.setPaymentUrl(paymentUrl);
            paymentTransactionRepository.save(transaction);

            log.info("Payment URL generated successfully for order: {}", order.getId());

            return PaymentResponse.builder()
                    .orderId(order.getId())
                    .paymentMethod(paymentMethod.name())
                    .paymentStatus(order.getPaymentStatus().name())
                    .paymentUrl(paymentUrl)
                    .amount(order.getTotalAmount())
                    .message("Please complete payment at the provided URL")
                    .build();

        } catch (Exception e) {
            log.error("Error initiating payment for order: {}", order.getId(), e);
            transaction.setPaymentStatus(PaymentStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            paymentTransactionRepository.save(transaction);
            throw new PaymentException("Failed to initiate payment", e);
        }
    }

    @Override
    @Transactional
    public PaymentResponse handlePaymentCallback(Long orderId, PaymentCallbackRequest callbackData) {
        return updatePaymentStatus(orderId,
                "SUCCESS".equalsIgnoreCase(callbackData.getStatus()) || "00".equals(callbackData.getStatus())
                        ? PaymentStatus.PAID
                        : PaymentStatus.FAILED,
                callbackData.getTransactionId());
    }

    @Override
    @Transactional
    public PaymentResponse handleVnPayCallback(Map<String, String> requestParams) {
        log.info("Handling VNPay callback: {}", requestParams);

        // 1. Verify Signature
        String vnpSecureHash = requestParams.get("vnp_SecureHash");
        if (vnpSecureHash == null) {
            throw new PaymentException("Missing vnp_SecureHash");
        }

        Map<String, String> verifyParams = new HashMap<>(requestParams);
        boolean isValid = VNPayUtil.verifySecureHash(verifyParams, vnPayConfig.getHashSecret(), vnpSecureHash);
        if (!isValid) {
            log.error("Invalid VNPay Signature. Params: {}", requestParams);
            throw new PaymentException("Invalid VNPay Signature");
        }

        // 2. Get Order ID from txnRef (format: ORDER_{id}_{timestamp})
        String txnRef = requestParams.get("vnp_TxnRef");
        long orderId;
        try {
            String[] parts = txnRef.split("_");
            orderId = Long.parseLong(parts[1]);
        } catch (Exception e) {
            throw new PaymentException("Invalid vnp_TxnRef format: " + txnRef);
        }

        // 3. Check Status
        String responseCode = requestParams.get("vnp_ResponseCode");
        PaymentStatus status = "00".equals(responseCode) ? PaymentStatus.PAID : PaymentStatus.FAILED;
        String transactionId = requestParams.get("vnp_TransactionNo");

        return updatePaymentStatus(orderId, status, transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse checkPaymentStatus(Long userId, Long orderId) {
        log.info("Checking payment status for user: {}, order: {}", userId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new PaymentException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new PaymentException("Unauthorized access to order");
        }

        return PaymentResponse.builder()
                .orderId(order.getId())
                .paymentMethod(order.getPaymentMethod().name())
                .paymentStatus(order.getPaymentStatus().name())
                .amount(order.getTotalAmount())
                .message("Current payment status")
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse processRefund(Long userId, Long orderId, String reason) {
        log.info("Processing refund for user: {}, order: {}, reason: {}", userId, orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new PaymentException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new PaymentException("Unauthorized access to order");
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new PaymentException("Cannot refund unpaid order");
        }

        if (order.getPaymentMethod() == PaymentMethod.COD) {
            throw new PaymentException("COD orders cannot be refunded online");
        }

        // TODO: Call actual refund API of VNPay
        log.warn("VNPay Refund API call not yet implemented, only updating status for order: {}", orderId);

        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setStatus(OrderStatus.REFUNDED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Create refund transaction record
        PaymentTransaction refundTransaction = PaymentTransaction.builder()
                .order(order)
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(PaymentStatus.REFUNDED)
                .amount(order.getTotalAmount())
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .gatewayResponse("Refund: " + reason)
                .build();
        paymentTransactionRepository.save(refundTransaction);

        log.info("Refund processed for order: {}", orderId);

        return PaymentResponse.builder()
                .orderId(order.getId())
                .paymentMethod(order.getPaymentMethod().name())
                .paymentStatus(PaymentStatus.REFUNDED.name())
                .amount(order.getTotalAmount())
                .message("Refund processed: " + reason)
                .build();
    }

    // ==================== Private Helpers ====================

    private PaymentResponse updatePaymentStatus(Long orderId, PaymentStatus newPaymentStatus, String transactionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new PaymentException("Order not found"));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.warn("Order {} is already paid", orderId);
            return PaymentResponse.builder()
                    .orderId(order.getId())
                    .paymentMethod(order.getPaymentMethod().name())
                    .paymentStatus(PaymentStatus.PAID.name())
                    .amount(order.getTotalAmount())
                    .message("Order already paid")
                    .build();
        }

        if (newPaymentStatus == PaymentStatus.PAID) {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
                eventPublisher.publishEvent(new com.example.MyWeb.event.OrderStatusChangedEvent(this, order, OrderStatus.PENDING, OrderStatus.CONFIRMED));
            }
            log.info("Payment successful for order: {}", orderId);
        } else {
            log.warn("Payment failed for order: {}", orderId);
        }

        order.setPaymentStatus(newPaymentStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Update payment transaction
        PaymentTransaction transaction = paymentTransactionRepository
                .findFirstByOrder_IdOrderByCreatedAtDesc(orderId)
                .orElse(PaymentTransaction.builder()
                        .order(order)
                        .paymentMethod(order.getPaymentMethod())
                        .amount(order.getTotalAmount())
                        .createdAt(LocalDateTime.now())
                        .build());

        transaction.setPaymentStatus(newPaymentStatus);
        transaction.setTransactionId(transactionId);
        transaction.setUpdatedAt(LocalDateTime.now());
        if (newPaymentStatus == PaymentStatus.PAID) {
            transaction.setCompletedAt(LocalDateTime.now());
        }
        paymentTransactionRepository.save(transaction);

        return PaymentResponse.builder()
                .orderId(order.getId())
                .paymentMethod(order.getPaymentMethod().name())
                .paymentStatus(newPaymentStatus.name())
                .amount(order.getTotalAmount())
                .transactionId(transactionId)
                .message(newPaymentStatus == PaymentStatus.PAID ? "Payment successful" : "Payment failed")
                .build();
    }

    private String generateVNPayUrl(Order order, String returnUrl) {
        try {
            Map<String, String> vnpParams = new HashMap<>();

            vnpParams.put("vnp_Version", vnPayConfig.getVersion());
            vnpParams.put("vnp_Command", vnPayConfig.getCommand());
            vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());

            long amount = (long) (order.getTotalAmount() * 100);
            vnpParams.put("vnp_Amount", String.valueOf(amount));

            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", "ORDER_" + order.getId() + "_" + System.currentTimeMillis());
            vnpParams.put("vnp_OrderInfo", "Thanh toan don hang #" + order.getId());
            vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
            vnpParams.put("vnp_Locale", "vn");

            String finalReturnUrl = returnUrl != null ? returnUrl : vnPayConfig.getReturnUrl();
            vnpParams.put("vnp_ReturnUrl", finalReturnUrl);

            vnpParams.put("vnp_IpAddr", "127.0.0.1");

            Date now = new Date();
            vnpParams.put("vnp_CreateDate", VNPayUtil.getVNPayDateFormat(now));

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            calendar.add(Calendar.MINUTE, 15);
            vnpParams.put("vnp_ExpireDate", VNPayUtil.getVNPayDateFormat(calendar.getTime()));

            String secureHash = VNPayUtil.generateSecureHash(vnpParams, vnPayConfig.getHashSecret());

            StringBuilder urlBuilder = new StringBuilder(vnPayConfig.getVnpayUrl());
            urlBuilder.append("?");

            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);

            for (String fieldName : fieldNames) {
                String fieldValue = vnpParams.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    urlBuilder.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    urlBuilder.append('=');
                    urlBuilder.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    urlBuilder.append('&');
                }
            }

            urlBuilder.append("vnp_SecureHash=");
            urlBuilder.append(secureHash);

            return urlBuilder.toString();

        } catch (UnsupportedEncodingException e) {
            throw new PaymentException("Error generating VNPay URL", e);
        }
    }
}
