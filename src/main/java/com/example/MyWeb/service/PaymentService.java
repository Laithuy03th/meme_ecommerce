package com.example.MyWeb.service;

import com.example.MyWeb.dto.payment.PaymentRequest;
import com.example.MyWeb.dto.payment.PaymentResponse;
import com.example.MyWeb.dto.payment.PaymentCallbackRequest;

public interface PaymentService {

    /**
     * Khởi tạo thanh toán cho một đơn hàng
     * 
     * @param userId  ID người dùng
     * @param request Thông tin yêu cầu thanh toán
     * @return PaymentResponse chứa URL thanh toán (nếu là online payment)
     */
    PaymentResponse initiatePayment(Long userId, PaymentRequest request);

    /**
     * Xử lý callback từ payment gateway (VNPay, Momo...)
     * 
     * @param orderId      ID đơn hàng
     * @param callbackData Dữ liệu từ payment gateway
     * @return PaymentResponse kết quả xử lý
     */
    PaymentResponse handlePaymentCallback(Long orderId, PaymentCallbackRequest callbackData);

    /**
     * Xử lý callback từ VNPay (Redirect hoặc IPN)
     *
     * @param requestParams Các tham số trả về từ VNPay
     * @return PaymentResponse kết quả xử lý
     */
    PaymentResponse handleVnPayCallback(java.util.Map<String, String> requestParams);

    /**
     * Kiểm tra trạng thái thanh toán của đơn hàng
     * 
     * @param userId  ID người dùng
     * @param orderId ID đơn hàng
     * @return PaymentResponse trạng thái hiện tại
     */
    PaymentResponse checkPaymentStatus(Long userId, Long orderId);

    /**
     * Xử lý hoàn tiền cho đơn hàng
     * 
     * @param userId  ID người dùng
     * @param orderId ID đơn hàng
     * @param reason  Lý do hoàn tiền
     * @return PaymentResponse kết quả hoàn tiền
     */
    PaymentResponse processRefund(Long userId, Long orderId, String reason);
}
