package com.example.MyWeb.service;

import com.example.MyWeb.config.VNPayConfig;
import com.example.MyWeb.dto.payment.PaymentRequest;
import com.example.MyWeb.dto.payment.PaymentResponse;
import com.example.MyWeb.exception.PaymentException;
import com.example.MyWeb.model.Order;
import com.example.MyWeb.model.PaymentTransaction;
import com.example.MyWeb.model.User;
import com.example.MyWeb.model.enums.OrderStatus;
import com.example.MyWeb.model.enums.PaymentMethod;
import com.example.MyWeb.model.enums.PaymentStatus;
import com.example.MyWeb.repository.OrderRepository;
import com.example.MyWeb.repository.PaymentTransactionRepository;
import com.example.MyWeb.service.impl.PaymentServiceImpl;
import com.example.MyWeb.util.VNPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit Test: PaymentServiceImpl
 *
 * Phạm vi kiểm thử:
 *  - TC-PAY-01: Tạo thanh toán COD thành công
 *  - TC-PAY-02: Tạo URL thanh toán VNPay thành công
 *  - TC-PAY-03: Đơn hàng không tồn tại → ném PaymentException
 *  - TC-PAY-04: Đơn hàng đã thanh toán → ném PaymentException
 *  - TC-PAY-05: Callback VNPay checksum hợp lệ → cập nhật PAID
 *  - TC-PAY-06: Callback VNPay checksum sai → ném PaymentException
 *  - TC-PAY-07: Callback VNPay mã thất bại (ResponseCode ≠ "00") → cập nhật FAILED
 *  - TC-PAY-08: Callback gửi lại khi đã PAID → không cập nhật lại (idempotency)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TC-PAY: Kiểm thử PaymentService – Thanh toán & VNPay Callback")
class PaymentServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private VNPayConfig vnPayConfig;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private static final Long USER_ID  = 1L;
    private static final Long ORDER_ID = 10L;

    private User testUser;
    private Order codOrder;
    private Order vnpayOrder;

    private static final String HASH_SECRET = "TEST_SECRET_KEY_32_CHARS_ABCDEFGH";

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(USER_ID).email("user@memeshop.vn").build();

        codOrder = Order.builder()
                .id(ORDER_ID)
                .user(testUser)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.COD)
                .totalAmount(500_000.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        vnpayOrder = Order.builder()
                .id(ORDER_ID)
                .user(testUser)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.VNPAY)
                .totalAmount(500_000.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // TC-PAY-01: Thanh toán COD thành công
    // =========================================================================
    @Test
    @DisplayName("TC-PAY-01: Thanh toán COD → trả về response với status UNPAID (chờ nhận hàng)")
    void initiatePayment_cod_shouldReturnUnpaidResponse() {
        // Arrange
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(codOrder));
        when(paymentTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentRequest req = new PaymentRequest();
        req.setOrderId(ORDER_ID);
        req.setReturnUrl("https://memeshop.vn/payment/return");

        // Act
        PaymentResponse result = paymentService.initiatePayment(USER_ID, req);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getPaymentMethod()).isEqualTo("COD");
        assertThat(result.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(result.getPaymentUrl()).isNull();

        verify(paymentTransactionRepository, times(1)).save(any());
    }

    // =========================================================================
    // TC-PAY-02: Tạo URL VNPay thành công
    // =========================================================================
    @Test
    @DisplayName("TC-PAY-02: Thanh toán VNPay → trả về paymentUrl chứa vnp_SecureHash")
    void initiatePayment_vnpay_shouldReturnUrlContainingSecureHash() {
        // Arrange
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(vnpayOrder));
        when(paymentTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Mock VNPayConfig
        when(vnPayConfig.getVersion()).thenReturn("2.1.0");
        when(vnPayConfig.getCommand()).thenReturn("pay");
        when(vnPayConfig.getTmnCode()).thenReturn("MEMESHOP");
        when(vnPayConfig.getOrderType()).thenReturn("other");
        when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);
        when(vnPayConfig.getVnpayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        when(vnPayConfig.getReturnUrl()).thenReturn("https://memeshop.vn/payment/return");

        PaymentRequest req = new PaymentRequest();
        req.setOrderId(ORDER_ID);
        req.setReturnUrl("https://memeshop.vn/payment/return");

        // Act
        PaymentResponse result = paymentService.initiatePayment(USER_ID, req);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPaymentUrl()).isNotNull();
        assertThat(result.getPaymentUrl()).contains("vnp_SecureHash");
        assertThat(result.getPaymentUrl()).contains("vnpayment.vn");
    }

    // =========================================================================
    // TC-PAY-03: Đơn hàng không tồn tại
    // =========================================================================
    @Test
    @DisplayName("TC-PAY-03: Đơn hàng không tồn tại → ném PaymentException \"Order not found\"")
    void initiatePayment_whenOrderNotFound_shouldThrowPaymentException() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        PaymentRequest req = new PaymentRequest();
        req.setOrderId(ORDER_ID);
        req.setReturnUrl("https://memeshop.vn/return");

        assertThatThrownBy(() -> paymentService.initiatePayment(USER_ID, req))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Order not found");
    }

    // =========================================================================
    // TC-PAY-04: Đơn hàng đã PAID → từ chối thanh toán lại
    // =========================================================================
    @Test
    @DisplayName("TC-PAY-04: Đơn hàng đã PAID → ném PaymentException \"already paid\"")
    void initiatePayment_whenOrderAlreadyPaid_shouldThrowPaymentException() {
        codOrder.setPaymentStatus(PaymentStatus.PAID);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(codOrder));

        PaymentRequest req = new PaymentRequest();
        req.setOrderId(ORDER_ID);
        req.setReturnUrl("https://memeshop.vn/return");

        assertThatThrownBy(() -> paymentService.initiatePayment(USER_ID, req))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("already paid");
    }


    // =========================================================================
    // TC-PAY-05: VNPay callback checksum hợp lệ, ResponseCode = "00" → PAID
    // =========================================================================
    @Test
    @DisplayName("TC-PAY-05: Callback VNPay hợp lệ (mã 00) → cập nhật trạng thái PAID")
    void handleVnPayCallback_withValidSignature_shouldMarkAsPaid() {
        // Arrange
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(vnpayOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.findFirstByOrder_IdOrderByCreatedAtDesc(ORDER_ID))
                .thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Tạo params hợp lệ và ký bằng HASH_SECRET
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "ORDER_" + ORDER_ID + "_1234567890");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "TXN-9876");
        params.put("vnp_Amount", "50000000");

        String correctHash = VNPayUtil.generateSecureHash(new HashMap<>(params), HASH_SECRET);
        params.put("vnp_SecureHash", correctHash);

        when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);

        // Act
        PaymentResponse result = paymentService.handleVnPayCallback(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPaymentStatus()).isEqualTo("PAID");

        verify(orderRepository).save(argThat(o ->
                o.getPaymentStatus() == PaymentStatus.PAID));
    }

    // =========================================================================
    // TC-PAY-06: Callback VNPay checksum sai → từ chối
    // =========================================================================
    @Test
    @DisplayName("TC-PAY-06: Callback VNPay checksum sai → ném PaymentException \"Invalid VNPay Signature\"")
    void handleVnPayCallback_withInvalidSignature_shouldThrowException() {
        when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "ORDER_" + ORDER_ID + "_1234567890");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "TXN-9876");
        params.put("vnp_SecureHash", "INVALID_HASH_VALUE_THAT_WILL_FAIL_VERIFICATION");

        assertThatThrownBy(() -> paymentService.handleVnPayCallback(params))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Invalid VNPay Signature");

        // Không cập nhật gì vào database
        verify(orderRepository, never()).save(any());
    }

    // =========================================================================
    // TC-PAY-07: VNPay trả ResponseCode ≠ "00" → cập nhật FAILED
    // =========================================================================
    @Test
    @DisplayName("TC-PAY-07: VNPay trả mã thất bại (ResponseCode=51) → cập nhật trạng thái FAILED")
    void handleVnPayCallback_withFailureResponseCode_shouldMarkAsFailed() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(vnpayOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.findFirstByOrder_IdOrderByCreatedAtDesc(ORDER_ID))
                .thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "ORDER_" + ORDER_ID + "_1234567890");
        params.put("vnp_ResponseCode", "51"); // 51 = số dư không đủ
        params.put("vnp_TransactionNo", "TXN-FAIL-001");
        params.put("vnp_Amount", "50000000");

        String correctHash = VNPayUtil.generateSecureHash(new HashMap<>(params), HASH_SECRET);
        params.put("vnp_SecureHash", correctHash);

        when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);

        // Act
        PaymentResponse result = paymentService.handleVnPayCallback(params);

        // Assert
        assertThat(result.getPaymentStatus()).isEqualTo("FAILED");
        verify(orderRepository).save(argThat(o ->
                o.getPaymentStatus() == PaymentStatus.FAILED));
    }

    // =========================================================================
    // TC-PAY-08: Callback gửi lại khi đơn đã PAID → không cập nhật (idempotency)
    // =========================================================================
    @Test
    @DisplayName("TC-PAY-08: Callback trùng lặp khi đơn đã PAID → không ghi đè, trả response ổn định")
    void handleVnPayCallback_whenAlreadyPaid_shouldNotOverwrite() {
        // Arrange – đơn đã PAID
        vnpayOrder.setPaymentStatus(PaymentStatus.PAID);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(vnpayOrder));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "ORDER_" + ORDER_ID + "_9999999999");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "TXN-REPEAT-001");
        params.put("vnp_Amount", "50000000");

        String correctHash = VNPayUtil.generateSecureHash(new HashMap<>(params), HASH_SECRET);
        params.put("vnp_SecureHash", correctHash);

        when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);

        // Act
        PaymentResponse result = paymentService.handleVnPayCallback(params);

        // Assert – status vẫn PAID, không gọi save thêm lần nào
        assertThat(result.getPaymentStatus()).isEqualTo("PAID");
        verify(orderRepository, never()).save(any());
    }
}
