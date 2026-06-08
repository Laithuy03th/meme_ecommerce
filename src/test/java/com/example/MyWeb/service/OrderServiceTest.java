package com.example.MyWeb.service;

import com.example.MyWeb.dto.order.CheckoutRequest;
import com.example.MyWeb.dto.order.OrderResponse;
import com.example.MyWeb.model.*;
import com.example.MyWeb.model.enums.*;
import com.example.MyWeb.repository.*;
import com.example.MyWeb.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test: OrderServiceImpl
 *
 * Phạm vi kiểm thử:
 *  - TC-CO-01: Checkout thành công với giỏ hàng hợp lệ
 *  - TC-CO-02: Giỏ hàng rỗng → ném ngoại lệ
 *  - TC-CO-03: Sản phẩm không tồn tại / variant null
 *  - TC-CO-04: Tồn kho không đủ (variant) → ném ngoại lệ
 *  - TC-CO-05: Voucher không hợp lệ (hết hạn) → ném ngoại lệ
 *  - TC-CO-06: Idempotency key trùng → không tạo đơn mới
 *  - TC-CO-07: Hủy đơn ở trạng thái PENDING → thành công
 *  - TC-CO-08: Hủy đơn đã SHIPPED → ném ngoại lệ
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TC-CO: Kiểm thử OrderService – Đặt hàng & Hủy đơn")
class OrderServiceTest {

    // ============================  Mocks  ============================
    @Mock private CartRepository cartRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private VoucherRepository voucherRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private StockService stockService;
    @Mock private ShippingFeeService shippingFeeService;
    @Mock private ShippingMethodRepository shippingMethodRepository;
    @Mock private com.example.MyWeb.repository.OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock private EmailService emailService;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ============================  Fixtures  ============================
    private static final Long USER_ID     = 1L;
    private static final Long ADDRESS_ID  = 10L;
    private static final Long VARIANT_ID  = 100L;
    private static final Long PRODUCT_ID  = 200L;
    private static final Long ORDER_ID    = 999L;
    private static final Long SHIPPING_ID = 5L;

    private User         testUser;
    private Address      testAddress;
    private Product      testProduct;
    private ProductVariant testVariant;
    private Cart         testCart;
    private CartItem     testCartItem;
    private ShippingMethod testShipping;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .email("test@memeshop.vn")
                .build();

        testAddress = Address.builder()
                .id(ADDRESS_ID)
                .fullName("Nguyễn Văn A")
                .addressLine1("123 Đường ABC")
                .district("Quận 1")
                .province("TP.HCM")
                .phone("0901234567")
                .build();

        testProduct = Product.builder()
                .id(PRODUCT_ID)
                .name("Áo phông MemeShop")
                .basePrice(250_000.0)
                .slug("ao-phong-memeshop")
                .thumbnailUrl("https://cdn.memeshop.vn/img.jpg")
                .build();

        testVariant = ProductVariant.builder()
                .id(VARIANT_ID)
                .product(testProduct)
                .color("Đen")
                .size("L")
                .stock(10)
                .price(270_000.0)
                .build();

        testCartItem = CartItem.builder()
                .id(1L)
                .product(testProduct)
                .variant(testVariant)
                .quantity(2)
                .unitPrice(270_000.0)
                .totalPrice(540_000.0)
                .createdAt(LocalDateTime.now())
                .build();

        testCart = Cart.builder()
                .id(1L)
                .user(testUser)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>(List.of(testCartItem)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testShipping = ShippingMethod.builder()
                .id(SHIPPING_ID)
                .name("Giao hàng tiêu chuẩn")
                .build();
    }

    // =========================================================================
    // TC-CO-01: Checkout thành công
    // =========================================================================
    @Test
    @DisplayName("TC-CO-01: Checkout hợp lệ → tạo đơn hàng, trừ tồn kho, xóa giỏ")
    void checkout_withValidCart_shouldCreateOrderAndDecreaseStock() {
        // Arrange
        CheckoutRequest req = buildValidCheckoutRequest();

        when(cartRepository.findByUser_IdAndStatus(USER_ID, CartStatus.ACTIVE))
                .thenReturn(Optional.of(testCart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(testAddress));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(testUser));
        when(shippingMethodRepository.findById(SHIPPING_ID))
                .thenReturn(Optional.of(testShipping));
        when(shippingFeeService.calculateFee(any(), any(), anyDouble(), any(), any()))
                .thenReturn(30_000.0);
        // atomicDecreaseStock thành công: trả 1 row affected
        when(productVariantRepository.atomicDecreaseStock(VARIANT_ID, 2))
                .thenReturn(1);
        Order savedOrder = buildSavedOrder();
        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);
        when(cartRepository.save(any(Cart.class)))
                .thenReturn(testCart);
        when(orderStatusHistoryRepository.save(any()))
                .thenReturn(null);

        // Act
        OrderResponse result = orderService.checkout(USER_ID, req);

        // Assert – đơn hàng được tạo
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PENDING");

        // Assert – atomicDecreaseStock được gọi đúng 1 lần với đúng tham số
        verify(productVariantRepository, times(1))
                .atomicDecreaseStock(VARIANT_ID, 2);
    }

    // =========================================================================
    // TC-CO-02: Giỏ hàng rỗng
    // =========================================================================
    @Test
    @DisplayName("TC-CO-02: Giỏ hàng rỗng → ném RuntimeException \"Cart is empty\"")
    void checkout_withEmptyCart_shouldThrowException() {
        // Arrange – cart không có items
        testCart.setItems(new ArrayList<>());
        when(cartRepository.findByUser_IdAndStatus(USER_ID, CartStatus.ACTIVE))
                .thenReturn(Optional.of(testCart));

        CheckoutRequest req = buildValidCheckoutRequest();

        // Act & Assert
        assertThatThrownBy(() -> orderService.checkout(USER_ID, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("empty");

        // Không bao giờ tới bước lưu đơn
        verify(orderRepository, never()).save(any());
    }

    // =========================================================================
    // TC-CO-03: Không tìm thấy giỏ hàng (cart không tồn tại)
    // =========================================================================
    @Test
    @DisplayName("TC-CO-03: Không tìm thấy giỏ hàng active → ném RuntimeException")
    void checkout_whenCartNotFound_shouldThrowException() {
        when(cartRepository.findByUser_IdAndStatus(USER_ID, CartStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.checkout(USER_ID, buildValidCheckoutRequest()))
                .isInstanceOf(RuntimeException.class);

        verify(orderRepository, never()).save(any());
    }

    // =========================================================================
    // TC-CO-04: Tồn kho không đủ (atomicDecreaseStock trả 0 row)
    // =========================================================================
    @Test
    @DisplayName("TC-CO-04: Tồn kho không đủ (race condition) → ném RuntimeException \"Out of stock\"")
    void checkout_whenStockInsufficient_shouldThrowException() {
        // Arrange – pre-check pass (stock=10 ≥ qty=2),
        // nhưng atomicDecreaseStock trả 0 (hàng vừa hết do concurrent request)
        CheckoutRequest req = buildValidCheckoutRequest();

        when(cartRepository.findByUser_IdAndStatus(USER_ID, CartStatus.ACTIVE))
                .thenReturn(Optional.of(testCart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(testAddress));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(testUser));
        when(shippingMethodRepository.findById(SHIPPING_ID))
                .thenReturn(Optional.of(testShipping));
        when(shippingFeeService.calculateFee(any(), any(), anyDouble(), any(), any()))
                .thenReturn(30_000.0);
        // Atomic SQL trả 0 → hết hàng
        when(productVariantRepository.atomicDecreaseStock(VARIANT_ID, 2))
                .thenReturn(0);

        // Act & Assert
        assertThatThrownBy(() -> orderService.checkout(USER_ID, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Out of stock");

        // Không được lưu đơn khi hết hàng
        verify(orderRepository, never()).save(any());
    }

    // =========================================================================
    // TC-CO-05: Voucher hết hạn / không hợp lệ
    // =========================================================================
    @Test
    @DisplayName("TC-CO-05: Voucher không hợp lệ (isValid=false) → ném RuntimeException")
    void checkout_withInvalidVoucher_shouldThrowException() {
        // Arrange – voucher tồn tại nhưng đã hết hạn
        Voucher expiredVoucher = new Voucher();
        expiredVoucher.setId(1L);
        expiredVoucher.setCode("EXPIRED10");
        expiredVoucher.setDiscountType(DiscountType.PERCENT);
        expiredVoucher.setDiscountValue(10.0);
        expiredVoucher.setUsageLimit(100);
        expiredVoucher.setUsedCount(100); // Đã dùng hết
        expiredVoucher.setIsActive(true);
        expiredVoucher.setStartDate(LocalDateTime.now().minusDays(30));
        expiredVoucher.setEndDate(LocalDateTime.now().minusDays(1)); // Đã hết hạn
        expiredVoucher.setCreatedAt(LocalDateTime.now());
        expiredVoucher.setUpdatedAt(LocalDateTime.now());


        CheckoutRequest req = buildValidCheckoutRequest();
        req.setVoucherCode("EXPIRED10");

        when(cartRepository.findByUser_IdAndStatus(USER_ID, CartStatus.ACTIVE))
                .thenReturn(Optional.of(testCart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(testAddress));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(testUser));
        when(shippingMethodRepository.findById(SHIPPING_ID))
                .thenReturn(Optional.of(testShipping));
        when(shippingFeeService.calculateFee(any(), any(), anyDouble(), any(), any()))
                .thenReturn(30_000.0);
        when(voucherRepository.findByCode("EXPIRED10"))
                .thenReturn(Optional.of(expiredVoucher));

        // Act & Assert
        assertThatThrownBy(() -> orderService.checkout(USER_ID, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invalid");

        verify(orderRepository, never()).save(any());
    }

    // =========================================================================
    // TC-CO-06: Idempotency key trùng → trả đơn cũ, không tạo mới
    // =========================================================================
    @Test
    @DisplayName("TC-CO-06: Idempotency key trùng → trả đơn đã tồn tại, không gọi save")
    void checkout_withDuplicateIdempotencyKey_shouldReturnExistingOrder() {
        // Arrange
        String idemKey = "KEY-12345-ABC";
        CheckoutRequest req = buildValidCheckoutRequest();
        req.setIdempotencyKey(idemKey);

        Order existingOrder = buildSavedOrder();
        existingOrder.setIdempotencyKey(idemKey);

        when(cartRepository.findByUser_IdAndStatus(USER_ID, CartStatus.ACTIVE))
                .thenReturn(Optional.of(testCart));
        when(orderRepository.findByIdempotencyKeyAndUser_Id(idemKey, USER_ID))
                .thenReturn(Optional.of(existingOrder));

        // Act
        OrderResponse result = orderService.checkout(USER_ID, req);

        // Assert – trả về đơn cũ, không tạo mới
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ORDER_ID);

        // Không được gọi atomicDecreaseStock hay orderRepository.save
        verify(productVariantRepository, never()).atomicDecreaseStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    // =========================================================================
    // TC-CO-07: Hủy đơn PENDING thành công → hoàn trả tồn kho
    // =========================================================================
    @Test
    @DisplayName("TC-CO-07: Hủy đơn PENDING → trạng thái CANCELED, hoàn trả tồn kho variant")
    void cancelOrder_withPendingOrder_shouldCancelAndRestoreStock() {
        // Arrange
        Order pendingOrder = buildSavedOrder();
        pendingOrder.setStatus(OrderStatus.PENDING);

        OrderItem oi = OrderItem.builder()
                .id(1L)
                .order(pendingOrder)
                .product(testProduct)
                .variant(testVariant)
                .quantity(2)
                .unitPrice(270_000.0)
                .totalPrice(540_000.0)
                .build();
        pendingOrder.setItems(new ArrayList<>(List.of(oi)));

        when(orderRepository.findByIdAndUser_Id(ORDER_ID, USER_ID))
                .thenReturn(Optional.of(pendingOrder));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(orderStatusHistoryRepository.save(any())).thenReturn(null);

        // Act
        OrderResponse result = orderService.cancelOrder(USER_ID, ORDER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("CANCELED");

        // Tồn kho variant phải được hoàn lại: 10 + 2 = 12
        verify(productVariantRepository).save(argThat(v ->
                v.getStock() == 12));
    }

    // =========================================================================
    // TC-CO-08: Hủy đơn đã SHIPPED → ném ngoại lệ
    // =========================================================================
    @Test
    @DisplayName("TC-CO-08: Hủy đơn đã SHIPPED → ném RuntimeException không cho hủy")
    void cancelOrder_withShippedOrder_shouldThrowException() {
        // Arrange
        Order shippedOrder = buildSavedOrder();
        shippedOrder.setStatus(OrderStatus.SHIPPED);
        shippedOrder.setItems(new ArrayList<>());

        when(orderRepository.findByIdAndUser_Id(ORDER_ID, USER_ID))
                .thenReturn(Optional.of(shippedOrder));

        // Act & Assert
        assertThatThrownBy(() -> orderService.cancelOrder(USER_ID, ORDER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot cancel");

        // Không được thay đổi tồn kho
        verify(productVariantRepository, never()).save(any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private CheckoutRequest buildValidCheckoutRequest() {
        CheckoutRequest req = new CheckoutRequest();
        req.setAddressId(ADDRESS_ID);
        req.setShippingMethodId(SHIPPING_ID);
        req.setPaymentMethod("COD");
        return req;
    }

    private Order buildSavedOrder() {
        return Order.builder()
                .id(ORDER_ID)
                .user(testUser)
                .address(testAddress)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.COD)
                .totalAmount(600_000.0)
                .shippingFee(30_000.0)
                .discountAmount(0.0)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
