package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.order.CheckoutRequest;
import com.example.MyWeb.dto.order.OrderItemResponse;
import com.example.MyWeb.dto.order.OrderResponse;
import com.example.MyWeb.model.*;
import com.example.MyWeb.repository.*;
import com.example.MyWeb.service.OrderService;
import com.example.MyWeb.service.StockService;
import com.example.MyWeb.service.ShippingFeeService;
import com.example.MyWeb.repository.ShippingMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.example.MyWeb.model.enums.CartStatus;
import com.example.MyWeb.model.enums.OrderStatus;
import com.example.MyWeb.model.enums.PaymentMethod;
import com.example.MyWeb.model.enums.PaymentStatus;

import com.example.MyWeb.model.Voucher;
import com.example.MyWeb.repository.VoucherRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

        private final CartRepository cartRepository;
        private final OrderRepository orderRepository;
        private final AddressRepository addressRepository;
        private final UserRepository userRepository;
        private final VoucherRepository voucherRepository;
        private final ProductRepository productRepository;
        private final ProductVariantRepository productVariantRepository;

        // NEW: Inject StockService for proper stock management
        private final StockService stockService;
        private final ShippingFeeService shippingFeeService;
        private final ShippingMethodRepository shippingMethodRepository;
        private final com.example.MyWeb.repository.OrderStatusHistoryRepository orderStatusHistoryRepository;
        private final com.example.MyWeb.service.EmailService emailService;
        private final ReviewRepository reviewRepository;
        private final org.springframework.context.ApplicationEventPublisher eventPublisher;

        private OrderItemResponse toItemDto(OrderItem item) {
                Product p = item.getProduct();
                ProductVariant v = item.getVariant();

                return OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(p.getId())
                                .variantId(v != null ? v.getId() : null)
                                .productName(item.getProductName())
                                .productSlug(p.getSlug())
                                .thumbnailUrl(p.getThumbnailUrl())
                                .color(v != null ? v.getColor() : null)
                                .size(v != null ? v.getSize() : null)
                                .unitPrice(item.getUnitPrice())
                                .quantity(item.getQuantity())
                                .totalPrice(item.getTotalPrice())
                                .build();
        }

        private OrderResponse toOrderDto(Order order) {
                var items = order.getItems() != null
                                ? order.getItems().stream().map(this::toItemDto).toList()
                                : new ArrayList<OrderItemResponse>();

                return OrderResponse.builder()
                                .id(order.getId())
                                .status(order.getStatus().name())
                                .paymentStatus(order.getPaymentStatus().name())
                                .paymentMethod(order.getPaymentMethod().name())
                                .totalAmount(order.getTotalAmount())
                                .shippingFee(order.getShippingFee())
                                .note(order.getNote())
                                .createdAt(order.getCreatedAt())
                                .items(items)
                                .build();
        }

        @Override
        @Transactional
        public OrderResponse checkout(Long userId, CheckoutRequest request) {

                Cart cart = cartRepository.findByUser_IdAndStatus(userId, CartStatus.ACTIVE)
                                .orElseThrow(() -> new RuntimeException("Cart is empty"));

                if (cart.getItems() == null || cart.getItems().isEmpty()) {
                        throw new RuntimeException("Cart is empty");
                }

                // NEW: Filter items if selectedCartItemIds is provided
                java.util.List<CartItem> checkoutItems;
                if (request.getSelectedCartItemIds() != null && !request.getSelectedCartItemIds().isEmpty()) {
                        java.util.List<Long> selectedIds = request.getSelectedCartItemIds();
                        checkoutItems = cart.getItems().stream()
                                        .filter(item -> selectedIds.contains(item.getId()))
                                        .collect(java.util.stream.Collectors.toList());

                        if (checkoutItems.size() != selectedIds.size()) {
                                throw new RuntimeException("One or more selected items are not in your cart");
                        }
                } else {
                        // Default: Buy all
                        checkoutItems = new ArrayList<>(cart.getItems());
                }

                if (checkoutItems.isEmpty()) {
                        throw new RuntimeException("No items selected for checkout");
                }

                // [Tầng 4 - Deadlock Prevention] Tránh Deadlock khi Update Stock
                // Sắp xếp items theo ProductId và VariantId tăng dần. 
                // DB sẽ khóa dòng theo một chiều xuyên suốt mọi giao dịch, triệt tiêu khả năng 2 giao dịch khóa chéo nhau gây Deadlock.
                checkoutItems.sort((item1, item2) -> {
                        int pCmp = item1.getProduct().getId().compareTo(item2.getProduct().getId());
                        if (pCmp != 0) return pCmp;
                        
                        Long v1 = item1.getVariant() != null ? item1.getVariant().getId() : 0L;
                        Long v2 = item2.getVariant() != null ? item2.getVariant().getId() : 0L;
                        return v1.compareTo(v2);
                });

                // [Tầng 3 - Idempotency] Kiểm tra key để tránh tạo đơn trùng khi user bấm liên
                // tục
                if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
                        java.util.Optional<Order> existingOrder = orderRepository
                                        .findByIdempotencyKeyAndUser_Id(request.getIdempotencyKey(), userId);
                        if (existingOrder.isPresent()) {
                                log.warn("Duplicate checkout request detected. IdempotencyKey={}, UserId={}",
                                                request.getIdempotencyKey(), userId);
                                return toOrderDto(existingOrder.get());
                        }
                }

                // [Tầng 1 - Atomic SQL Update] Kiểm tra sơ bộ trước khi tạo đơn
                // Lưu ý: đây chỉ là kiểm tra có tham khảo, kiểm tra thực sự xảy ra
                // trong atomicDecreaseStock() bên dưới (chốt chặn cuối cùng).
                for (CartItem ci : checkoutItems) {
                        Product p = ci.getProduct();
                        ProductVariant v = ci.getVariant();
                        int qty = ci.getQuantity();
                        if (v != null) {
                                Integer variantStock = v.getStock() != null ? v.getStock() : 0;
                                if (variantStock < qty) {
                                        throw new RuntimeException("Out of stock: " + p.getName()
                                                        + " - " + v.getColor() + "/" + v.getSize()
                                                        + " (Available: " + variantStock + ", Requested: " + qty + ")");
                                }
                        } else {
                                stockService.validateStock(p.getId(), qty);
                        }
                }

                Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                                .orElseThrow(() -> new RuntimeException("Address not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                double itemsTotal = checkoutItems.stream()
                                .mapToDouble(CartItem::getTotalPrice)
                                .sum();

                // 1. Calculate Total Weight (Set to 0.0 as per business rule - simplified
                // shipping)
                double totalWeight = 0.0;

                // 2. Shipping Method
                ShippingMethod shippingMethod = shippingMethodRepository.findById(request.getShippingMethodId())
                                .orElseThrow(() -> new RuntimeException("Shipping method not found"));

                // 3. Payment Method (Parse here for Fee Calculation)
                PaymentMethod pm;
                try {
                        pm = PaymentMethod.valueOf(request.getPaymentMethod());
                } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid payment method: " + request.getPaymentMethod());
                }

                // 4. Voucher (Fetch for Free Shipping check)
                Voucher voucher = null;
                boolean shouldApplyVoucher = request.getVoucherCode() != null && !request.getVoucherCode().isBlank();
                if (shouldApplyVoucher) {
                        voucher = voucherRepository.findByCode(request.getVoucherCode().toUpperCase()).orElse(null);
                }

                // 4.1 Filter items for Voucher Category Check (using checkoutItems)
                // (Logic inside voucher checking below uses 'cart.getItems()', need to update
                // to 'checkoutItems')

                // 5. Calculate Fee
                double shippingFee = shippingFeeService.calculateFee(shippingMethod, address, totalWeight, pm, voucher);

                double discountAmount = 0.0;
                String voucherCode = null;

                // Apply voucher if provided (Product Discount)
                if (voucher != null) {

                        // 1. Validate basic rules
                        if (!voucher.isValid()) {
                                throw new RuntimeException("Voucher is invalid or expired");
                        }

                        // 2. Validate Usage Limit Per User
                        long userUsage = orderRepository.countByUser_IdAndVoucherCode(userId, voucher.getCode());
                        if (voucher.getUsageLimitPerUser() != null && userUsage >= voucher.getUsageLimitPerUser()) {
                                throw new RuntimeException("You have reached the usage limit for this voucher");
                        }

                        // 3. Calculate eligible amount based on Categories
                        double eligibleAmount = 0.0;
                        if (voucher.getApplicableCategoryIds() != null
                                        && !voucher.getApplicableCategoryIds().isBlank()) {
                                String[] catIdsToCheck = voucher.getApplicableCategoryIds().split(",");
                                // FIXED: Use checkoutItems
                                for (CartItem item : checkoutItems) {
                                        String itemCatId = String.valueOf(item.getProduct().getCategory().getId());
                                        boolean isMatch = false;
                                        for (String id : catIdsToCheck) {
                                                if (id.trim().equals(itemCatId)) {
                                                        isMatch = true;
                                                        break;
                                                }
                                        }
                                        if (isMatch) {
                                                eligibleAmount += item.getTotalPrice();
                                        }
                                }
                                if (eligibleAmount == 0) {
                                        // Voucher match no items.
                                        // If it also didn't give free shipping, then it's useless for this order.
                                        // We check if free shipping was applied? (hard to know here without boolean
                                        // flag)
                                        // Simple rule: If category mismatch -> Warning only (User might get Free Ship)
                                        // throw new RuntimeException("This voucher is not applicable to any items in
                                        // your cart");
                                }
                        } else {
                                eligibleAmount = itemsTotal; // Apply to all items
                        }

                        // 4. Validate Min Order Amount & Calculate Discount
                        if (eligibleAmount > 0) {
                                if (voucher.getMinOrderAmount() != null
                                                && eligibleAmount < voucher.getMinOrderAmount()) {
                                        throw new RuntimeException(
                                                        "Order amount does not meet minimum requirement for this voucher");
                                }
                                discountAmount = voucher.calculateDiscount(eligibleAmount);
                                voucherCode = voucher.getCode();

                                // FIX Race Condition: Dùng Atomic SQL UPDATE thay vì read-then-write
                                // Nếu 2 user checkout cùng lúc, chỉ 1 người được dùng voucher
                                int updated = voucherRepository.incrementUsedCount(voucher.getId());
                                if (updated == 0) {
                                    throw new RuntimeException("Voucher vừa hết lượt sử dụng. Vui lòng thử voucher khác!");
                                }
                        } else {
                                // If eligibleAmount is 0 but we are here, it means voucher valid but no product
                                // match.
                                // If it gave free shipping, we should still record usage and save voucherCode?
                                // Yes, if Free Shipping was applied.
                                if (Boolean.TRUE.equals(voucher.getFreeShipping())) {
                                        voucherCode = voucher.getCode();
                                        // FIX Race Condition: Dùng Atomic SQL UPDATE
                                        int updatedFreeShip = voucherRepository.incrementUsedCount(voucher.getId());
                                        if (updatedFreeShip == 0) {
                                            throw new RuntimeException("Voucher vừa hết lượt sử dụng. Vui lòng thử voucher khác!");
                                        }
                                } else {
                                        throw new RuntimeException(
                                                        "This voucher is not applicable to any items in your cart");
                                }
                        }
                }

                // FIX: Math.max(0.0,...) đảm bảo tổng tiền không bao giờ âm
                // (trường hợp FreeShipping + discount lớn cộng lại > itemsTotal)
                double totalAmount = Math.max(0.0, itemsTotal + shippingFee - discountAmount);

                LocalDateTime now = LocalDateTime.now();

                // PaymentMethod pm already parsed above

                Order order = Order.builder()
                                .user(user)
                                .address(address)
                                .status(OrderStatus.PENDING)
                                .paymentStatus(PaymentStatus.UNPAID)
                                .paymentMethod(pm)
                                .totalAmount(totalAmount)
                                .shippingFee(shippingFee)
                                .voucherCode(voucherCode)
                                .discountAmount(discountAmount)
                                .note(request.getNote())
                                .idempotencyKey(request.getIdempotencyKey()) // Idempotency
                                .createdAt(now)
                                .updatedAt(now)
                                .items(new ArrayList<>())
                                .build();

                // map cart_items -> order_items (snapshot) & decrease stock
                for (CartItem ci : checkoutItems) {
                        Product p = ci.getProduct();
                        ProductVariant v = ci.getVariant();
                        int qty = ci.getQuantity();

                        // [Tầng 1 - Atomic SQL Update] Trừ tồn kho nguyên tử — chốt chặn cuối cùng
                        // SQL: UPDATE ... WHERE id=? AND stock >= qty
                        // row affected = 0 → OutOfStockException
                        if (v != null) {
                                int rows = productVariantRepository.atomicDecreaseStock(v.getId(), qty);
                                if (rows == 0) {
                                        throw new RuntimeException("Out of stock (concurrent): " + p.getName()
                                                        + " - " + v.getColor() + "/" + v.getSize());
                                }
                                log.info("Atomic variant stock decreased: variantId={}, qty={}", v.getId(), qty);
                        } else {
                                // stockService.decreaseStock() dùng atomicDecreaseStock bên trong
                                stockService.decreaseStock(p.getId(), qty);
                        }

                        OrderItem oi = OrderItem.builder()
                                        .order(order)
                                        .product(p)
                                        .variant(v)
                                        .productName(p.getName())
                                        .unitPrice(ci.getUnitPrice())
                                        .quantity(qty)
                                        .totalPrice(ci.getTotalPrice())
                                        .createdAt(now)
                                        .build();
                        order.getItems().add(oi);
                }

                // Save order + items
                Order savedOrder = orderRepository.save(order);

                log.info("Order created successfully: orderId={}, userId={}, totalAmount={}",
                                savedOrder.getId(), userId, totalAmount);

                eventPublisher.publishEvent(new com.example.MyWeb.event.OrderStatusChangedEvent(this, savedOrder, null, OrderStatus.PENDING));

                orderStatusHistoryRepository.save(OrderStatusHistory.systemChange(savedOrder, OrderStatus.PENDING, OrderStatus.PENDING, "Order placed"));
                emailService.sendOrderConfirmation(user.getEmail(), savedOrder.getId(), totalAmount);

                // FIXED: Partial Checkout - Only remove purchased items
                cart.getItems().removeAll(checkoutItems);

                if (cart.getItems().isEmpty()) {

                }
                cart.setUpdatedAt(now);
                cartRepository.save(cart);

                return toOrderDto(savedOrder);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<com.example.MyWeb.dto.order.OrderListResponse> getMyOrders(Long userId, int page, int size) {
                Pageable pageable = PageRequest.of(page, size);
                Page<Order> orders = orderRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
                return orders.map(this::toListResponse);
        }

        @Override
        @Transactional(readOnly = true)
        public com.example.MyWeb.dto.order.OrderDetailResponse getMyOrderDetail(Long userId, Long orderId) {
                Order order = orderRepository.findByIdAndUser_Id(orderId, userId)
                                .orElseThrow(() -> new RuntimeException("Order not found"));
                // ensure items loaded
                order.getItems().size();
                return toDetailResponse(order);
        }

        private com.example.MyWeb.dto.order.OrderListResponse toListResponse(Order order) {
                String firstImage = null;
                int itemCount = 0;

                if (order.getItems() != null && !order.getItems().isEmpty()) {
                        itemCount = order.getItems().size();
                        // Safe: kiểm tra null khi product có thể đã bị xóa
                        for (OrderItem item : order.getItems()) {
                                try {
                                        if (item.getProduct() != null && item.getProduct().getThumbnailUrl() != null) {
                                                firstImage = item.getProduct().getThumbnailUrl();
                                                break;
                                        }
                                } catch (Exception e) {
                                        log.warn("Could not load product thumbnail for order {}: {}", order.getId(), e.getMessage());
                                }
                        }
                }

                return com.example.MyWeb.dto.order.OrderListResponse.builder()
                                .id(order.getId())
                                .orderNumber("ORD-" + order.getId())
                                .createdAt(order.getCreatedAt())
                                .status(order.getStatus().name())
                                .itemCount(itemCount)
                                .totalAmount(order.getTotalAmount())
                                .firstItemImageUrl(firstImage)
                                .build();
        }

        private com.example.MyWeb.dto.order.OrderDetailResponse toDetailResponse(Order order) {
                java.util.List<com.example.MyWeb.dto.order.OrderDetailResponse.TimelineStep> timeline = new ArrayList<>();
                java.util.List<OrderStatusHistory> histories = orderStatusHistoryRepository.findByOrder_IdOrderByCreatedAtAsc(order.getId());
                
                if (histories.isEmpty()) {
                        timeline.add(com.example.MyWeb.dto.order.OrderDetailResponse.TimelineStep.builder()
                                        .status("Order Placed")
                                        .timestamp(order.getCreatedAt())
                                        .completed(true)
                                        .build());
                } else {
                        for (OrderStatusHistory h : histories) {
                                timeline.add(com.example.MyWeb.dto.order.OrderDetailResponse.TimelineStep.builder()
                                                .status(h.getToStatus().name() + (h.getNote() != null ? " (" + h.getNote() + ")" : ""))
                                                .timestamp(h.getCreatedAt())
                                                .completed(true)
                                                .build());
                        }
                }

                java.util.List<com.example.MyWeb.dto.order.OrderDetailResponse.OrderItemDto> items = order.getItems()
                                .stream()
                                .map(item -> com.example.MyWeb.dto.order.OrderDetailResponse.OrderItemDto.builder()
                                                .id(item.getId())
                                                .productId(item.getProduct().getId())
                                                .productName(item.getProductName())
                                                .productImageUrl(item.getProduct().getThumbnailUrl())
                                                .variantInfo(item.getVariant() != null
                                                                ? "Color: " + item.getVariant().getColor() + ", Size: "
                                                                                + item.getVariant().getSize()
                                                                : "")
                                                .quantity(item.getQuantity())
                                                .price(item.getUnitPrice())
                                                .hasReviewed(reviewRepository.existsByUser_IdAndOrderItem_Id(order.getUser().getId(), item.getId()))
                                                .build())
                                .collect(java.util.stream.Collectors.toList());

                com.example.MyWeb.dto.order.OrderDetailResponse.ShippingAddressDto addressDto = null;
                if (order.getAddress() != null) {
                        addressDto = com.example.MyWeb.dto.order.OrderDetailResponse.ShippingAddressDto.builder()
                                        .fullName(order.getAddress().getFullName())
                                        .addressLine(order.getAddress().getAddressLine1() + ", "
                                                        + order.getAddress().getDistrict() + ", "
                                                        + order.getAddress().getProvince())
                                        .phone(order.getAddress().getPhone())
                                        .build();
                }

                com.example.MyWeb.dto.order.OrderDetailResponse.PaymentMethodDto paymentDto = com.example.MyWeb.dto.order.OrderDetailResponse.PaymentMethodDto
                                .builder()
                                .type(order.getPaymentMethod().name())
                                .last4("4242")
                                .build();

                return com.example.MyWeb.dto.order.OrderDetailResponse.builder()
                                .id(order.getId())
                                .orderNumber("ORD-" + order.getId())
                                .createdAt(order.getCreatedAt())
                                .status(order.getStatus().name())
                                .timeline(timeline)
                                .items(items)
                                .subtotal(order.getTotalAmount() - order.getShippingFee()
                                                + (order.getDiscountAmount() != null ? order.getDiscountAmount() : 0))
                                .shippingFee(order.getShippingFee())
                                .totalAmount(order.getTotalAmount())
                                .shippingAddress(addressDto)
                                .paymentMethod(paymentDto)
                                .deliveredAt(orderStatusHistoryRepository
                                                .findFirstByOrder_IdAndToStatusOrderByCreatedAtDesc(order.getId(), OrderStatus.DELIVERED)
                                                .map(OrderStatusHistory::getCreatedAt)
                                                .orElse(null))
                                .build();
        }

        @Override
        @Transactional
        public OrderResponse cancelOrder(Long userId, Long orderId) {
                Order order = orderRepository.findByIdAndUser_Id(orderId, userId)
                                .orElseThrow(() -> new RuntimeException("Order not found"));

                // Only allow cancel if order is PENDING or CONFIRMED
                if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
                        throw new RuntimeException("Cannot cancel order with status: " + order.getStatus());
                }

                // FIXED: Return stock to inventory
                for (OrderItem item : order.getItems()) {
                        Product product = item.getProduct();
                        ProductVariant variant = item.getVariant();
                        int quantity = item.getQuantity();

                        if (variant != null) {
                                // Return variant stock
                                Integer currentStock = variant.getStock() != null ? variant.getStock() : 0;
                                variant.setStock(currentStock + quantity);
                                productVariantRepository.save(variant);

                                log.info("Returned variant stock on cancel: product={}, variant={}, quantity={}",
                                                product.getId(), variant.getId(), quantity);
                        } else {
                                // Use StockService to return product stock
                                stockService.increaseStock(product.getId(), quantity);
                        }
                }

                OrderStatus oldStatus = order.getStatus();
                order.setStatus(OrderStatus.CANCELED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                orderStatusHistoryRepository.save(OrderStatusHistory.adminChange(order, oldStatus, OrderStatus.CANCELED, "CUSTOMER", "User canceled order"));
                eventPublisher.publishEvent(new com.example.MyWeb.event.OrderStatusChangedEvent(this, order, oldStatus, OrderStatus.CANCELED));
                emailService.sendOrderStatusUpdate(order.getUser().getEmail(), orderId, "CANCELED");

                log.info("Order cancelled and stock returned: orderId={}, userId={}", orderId, userId);

                return toOrderDto(order);
        }

        @Override
        @Transactional
        public OrderResponse requestReturn(Long userId, Long orderId, String reason) {
                Order order = orderRepository.findByIdAndUser_Id(orderId, userId)
                                .orElseThrow(() -> new RuntimeException("Order not found"));

                // Only allow return if order is DELIVERED
                if (order.getStatus() != OrderStatus.DELIVERED) {
                        throw new RuntimeException("Cannot request return for order with status: " + order.getStatus());
                }

                // Chinh sach: Toi da 7 ngay sau giao hang thi khong duoc tra hang
                // Lay thoi diem giao hang tu order_status_history
                java.util.Optional<OrderStatusHistory> deliveredHistory =
                        orderStatusHistoryRepository.findFirstByOrder_IdAndToStatusOrderByCreatedAtDesc(
                                orderId, OrderStatus.DELIVERED);

                // Fail-safe: neu khong tim thay record DELIVERED -> khong cho phep tra hang
                // (tranh truong hop don cu khong co history bi bypass validation)
                if (deliveredHistory.isEmpty()) {
                        log.warn("Cannot find DELIVERED history for orderId={}. Blocking return request as safety measure.", orderId);
                        throw new RuntimeException("Không thể xác nhận thời điểm giao hàng. Vui lòng liên hệ hỗ trợ để yêu cầu trả hàng.");
                }

                LocalDateTime deliveredAt = deliveredHistory.get().getCreatedAt();
                long daysSinceDelivered = java.time.temporal.ChronoUnit.DAYS.between(deliveredAt, LocalDateTime.now());
                if (daysSinceDelivered > 7) {
                        throw new RuntimeException(
                                "Đã quá 7 ngày kể từ ngày giao hàng (" + daysSinceDelivered + " ngày). Chính sách trả hàng chỉ áp dụng trong 7 ngày.");
                }

                OrderStatus oldStatus = order.getStatus();
                order.setStatus(OrderStatus.RETURN_REQUESTED);
                // Note: We might want to append the reason to the order note or a separate
                // field
                // For now, appending to note
                String currentNote = order.getNote() != null ? order.getNote() : "";
                order.setNote(currentNote + " [Return Reason: " + reason + "]");

                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                
                orderStatusHistoryRepository.save(OrderStatusHistory.adminChange(order, oldStatus, OrderStatus.RETURN_REQUESTED, "CUSTOMER", "User requested return: " + reason));

                log.info("Return requested: orderId={}, userId={}, reason={}", orderId, userId, reason);

                // NOTE: Stock will be returned after admin approves the return request
                // This should be handled in AdminOrderService.approveReturn()

                return toOrderDto(order);
        }

        @Override
        @Transactional
        public com.example.MyWeb.dto.cart.CartResponse reorder(Long userId, Long orderId) {
                // Get the old order
                Order oldOrder = orderRepository.findByIdAndUser_Id(orderId, userId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Order not found or you don't have permission"));

                // Get user's active cart (or create new one)
                Cart cart = cartRepository.findByUser_IdAndStatus(userId, CartStatus.ACTIVE)
                                .orElseGet(() -> {
                                        User user = userRepository.findById(userId)
                                                        .orElseThrow(() -> new RuntimeException("User not found"));

                                        Cart newCart = Cart.builder()
                                                        .user(user)
                                                        .status(CartStatus.ACTIVE)
                                                        .createdAt(LocalDateTime.now())
                                                        .updatedAt(LocalDateTime.now())
                                                        .items(new java.util.ArrayList<>())
                                                        .build();
                                        return cartRepository.save(newCart);
                                });

                // Add all items from old order to cart
                for (OrderItem orderItem : oldOrder.getItems()) {
                        Product product = orderItem.getProduct();
                        ProductVariant variant = orderItem.getVariant();
                        int quantity = orderItem.getQuantity();

                        // Validate stock availability
                        if (variant != null) {
                                Integer variantStock = variant.getStock() != null ? variant.getStock() : 0;
                                if (variantStock < quantity) {
                                        log.warn("Insufficient stock for variant: {}, requested: {}, available: {}",
                                                        variant.getId(), quantity, variantStock);
                                        continue;
                                }
                        } else {
                                Integer productStock = product.getStockQuantity() != null
                                                ? product.getStockQuantity()
                                                : 0;
                                if (productStock < quantity) {
                                        log.warn("Insufficient stock for product: {}, requested: {}, available: {}",
                                                        product.getId(), quantity, productStock);
                                        continue;
                                }
                        }

                        // Calculate price
                        double unitPrice = variant != null && variant.getPrice() != null
                                        ? variant.getPrice()
                                        : product.getBasePrice();

                        // Check if item already exists in cart
                        CartItem existingItem = null;
                        if (cart.getItems() != null) {
                                for (CartItem ci : cart.getItems()) {
                                        Long vId = (ci.getVariant() != null ? ci.getVariant().getId() : null);
                                        Long reqVId = (variant != null ? variant.getId() : null);

                                        if (ci.getProduct().getId().equals(product.getId())
                                                        && java.util.Objects.equals(vId, reqVId)) {
                                                existingItem = ci;
                                                break;
                                        }
                                }
                        }

                        LocalDateTime now = LocalDateTime.now();

                        if (existingItem != null) {
                                int newQty = existingItem.getQuantity() + quantity;
                                existingItem.setQuantity(newQty);
                                existingItem.setUnitPrice(unitPrice);
                                existingItem.setTotalPrice(unitPrice * newQty);
                                existingItem.setUpdatedAt(now);
                        } else {
                                CartItem newItem = CartItem.builder()
                                                .cart(cart)
                                                .product(product)
                                                .variant(variant)
                                                .quantity(quantity)
                                                .unitPrice(unitPrice)
                                                .totalPrice(unitPrice * quantity)
                                                .createdAt(now)
                                                .updatedAt(now)
                                                .build();

                                cart.getItems().add(newItem);
                        }
                }

                // Save cart
                cart.setUpdatedAt(LocalDateTime.now());
                cart = cartRepository.save(cart);

                log.info("Reorder completed: orderId={}, userId={}, items added", orderId, userId);

                // Build response
                java.util.List<com.example.MyWeb.dto.cart.CartItemResponse> itemDtos = cart.getItems() != null
                                ? cart.getItems().stream()
                                                .map(this::toCartItemDto)
                                                .collect(java.util.stream.Collectors.toList())
                                : new java.util.ArrayList<>();

                double total = itemDtos.stream()
                                .mapToDouble(com.example.MyWeb.dto.cart.CartItemResponse::getTotalPrice)
                                .sum();

                return com.example.MyWeb.dto.cart.CartResponse.builder()
                                .id(cart.getId())
                                .items(itemDtos)
                                .totalItems(itemDtos.size())
                                .totalAmount(total)
                                .build();
        }

        private com.example.MyWeb.dto.cart.CartItemResponse toCartItemDto(CartItem item) {
                Product p = item.getProduct();
                ProductVariant v = item.getVariant();

                return com.example.MyWeb.dto.cart.CartItemResponse.builder()
                                .id(item.getId())
                                .productId(p.getId())
                                .productName(p.getName())
                                .productSlug(p.getSlug())
                                .thumbnailUrl(p.getThumbnailUrl())
                                .variantId(v != null ? v.getId() : null)
                                .color(v != null ? v.getColor() : null)
                                .size(v != null ? v.getSize() : null)
                                .unitPrice(item.getUnitPrice())
                                .quantity(item.getQuantity())
                                .totalPrice(item.getTotalPrice())
                                .build();
        }

        @Override
        @Transactional
        public OrderResponse approveReturn(Long orderId) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

                if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
                        throw new RuntimeException("Order is not in RETURN_REQUESTED status");
                }

                // Return stock
                for (OrderItem item : order.getItems()) {
                        Product product = item.getProduct();
                        ProductVariant variant = item.getVariant();
                        int quantity = item.getQuantity();

                        if (variant != null) {
                                Integer currentStock = variant.getStock() != null ? variant.getStock() : 0;
                                variant.setStock(currentStock + quantity);
                                productVariantRepository.save(variant);
                        } else {
                                stockService.increaseStock(product.getId(), quantity);
                        }
                }

                OrderStatus oldStatus = order.getStatus();
                order.setStatus(OrderStatus.RETURNED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                
                orderStatusHistoryRepository.save(OrderStatusHistory.adminChange(order, oldStatus, OrderStatus.RETURNED, "ADMIN", "Return approved"));
                emailService.sendOrderStatusUpdate(order.getUser().getEmail(), orderId, "RETURNED");

                log.info("Order return approved and stock returned: orderId={}", orderId);
                return toOrderDto(order);
        }

        @Override
        @Transactional
        public OrderResponse updateOrderStatus(Long orderId, String newStatus) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found"));

                OrderStatus targetStatus;
                try {
                        targetStatus = OrderStatus.valueOf(newStatus.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid order status: " + newStatus);
                }

                OrderStatus currentStatus = order.getStatus();

                // ================================================================
                // STATE MACHINE: Chỉ cho phép các chuyển trạng thái hợp lệ
                // ================================================================
                if (!isValidTransition(currentStatus, targetStatus)) {
                        throw new RuntimeException(
                                "Invalid status transition: " + currentStatus + " -> " + targetStatus
                                + ". Allowed: " + getAllowedTransitions(currentStatus));
                }

                // ================================================================
                // Prevent confirming UNPAID online payment orders
                // ================================================================
                if (targetStatus == OrderStatus.CONFIRMED && 
                    order.getPaymentMethod() != PaymentMethod.COD && 
                    order.getPaymentStatus() != PaymentStatus.PAID) {
                        throw new RuntimeException("Cannot confirm an online payment order that has not been paid yet.");
                }

                // ================================================================
                // Hoàn stock khi chuyển sang CANCELED hoặc RETURNED
                // ================================================================
                if (targetStatus == OrderStatus.CANCELED || targetStatus == OrderStatus.RETURNED) {
                        for (OrderItem item : order.getItems()) {
                                ProductVariant variant = item.getVariant();
                                int quantity = item.getQuantity();
                                if (variant != null) {
                                        Integer currentStock = variant.getStock() != null ? variant.getStock() : 0;
                                        variant.setStock(currentStock + quantity);
                                        productVariantRepository.save(variant);
                                } else {
                                        stockService.increaseStock(item.getProduct().getId(), quantity);
                                }
                        }
                }

                // ================================================================
                // Cập nhật PaymentStatus tự động theo chuẩn e-commerce (Shopee)
                // ================================================================
                // 1. Nếu giao hàng thành công (DELIVERED) và là đơn COD -> Đánh dấu đã thanh toán
                if (targetStatus == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.COD) {
                        order.setPaymentStatus(PaymentStatus.PAID);
                        log.info("Order {} delivered via COD. PaymentStatus automatically updated to PAID.", orderId);
                }

                // 2. Nếu đơn bị Hủy (CANCELED) nhưng khách ĐÃ THANH TOÁN (VNPAY) -> Đổi sang REFUNDED để kế toán biết đường hoàn tiền
                if (targetStatus == OrderStatus.CANCELED && order.getPaymentStatus() == PaymentStatus.PAID) {
                        order.setPaymentStatus(PaymentStatus.REFUNDED);
                        log.info("Paid order {} was canceled. PaymentStatus automatically updated to REFUNDED.", orderId);
                }

                order.setStatus(targetStatus);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                
                eventPublisher.publishEvent(new com.example.MyWeb.event.OrderStatusChangedEvent(this, order, currentStatus, targetStatus));

                orderStatusHistoryRepository.save(OrderStatusHistory.adminChange(order, currentStatus, targetStatus, "ADMIN", "Status updated"));
                emailService.sendOrderStatusUpdate(order.getUser().getEmail(), orderId, targetStatus.name());
                log.info("Order status updated: orderId={}, {} -> {}", orderId, currentStatus, targetStatus);
                return toOrderDto(order);
        }

        /**
         * State Machine: Các chuyển trạng thái hợp lệ
         * Client: PENDING -> CANCELED, DELIVERED -> RETURN_REQUESTED
         * Admin: PENDING -> CONFIRMED -> PACKED -> SHIPPED -> DELIVERED -> RETURNED -> REFUNDED
         */
        private boolean isValidTransition(OrderStatus from, OrderStatus to) {
                return switch (from) {
                        case PENDING          -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELED;
                        case CONFIRMED        -> to == OrderStatus.PACKED    || to == OrderStatus.CANCELED;
                        case PACKED           -> to == OrderStatus.SHIPPED   || to == OrderStatus.CANCELED;
                        case SHIPPED          -> to == OrderStatus.DELIVERED;
                        case DELIVERED        -> to == OrderStatus.RETURN_REQUESTED || to == OrderStatus.REFUNDED;
                        case RETURN_REQUESTED -> to == OrderStatus.RETURNED  || to == OrderStatus.DELIVERED;
                        case RETURNED         -> to == OrderStatus.REFUNDED;
                        case CANCELED, REFUNDED -> false;
                };
        }

        private String getAllowedTransitions(OrderStatus from) {
                return switch (from) {
                        case PENDING          -> "CONFIRMED, CANCELED";
                        case CONFIRMED        -> "PACKED, CANCELED";
                        case PACKED           -> "SHIPPED, CANCELED";
                        case SHIPPED          -> "DELIVERED";
                        case DELIVERED        -> "RETURN_REQUESTED, REFUNDED";
                        case RETURN_REQUESTED -> "RETURNED, DELIVERED";
                        case RETURNED         -> "REFUNDED";
                        case CANCELED, REFUNDED -> "none (terminal state)";
                };
        }

        @Override
        @Transactional
        public void systemCancelOrder(Long orderId, String reason) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

                if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
                        return; // Only cancel PENDING or CONFIRMED
                }

                OrderStatus oldStatus = order.getStatus();

                for (OrderItem item : order.getItems()) {
                        ProductVariant variant = item.getVariant();
                        int quantity = item.getQuantity();

                        if (variant != null) {
                                Integer currentStock = variant.getStock() != null ? variant.getStock() : 0;
                                variant.setStock(currentStock + quantity);
                                productVariantRepository.save(variant);
                        } else {
                                stockService.increaseStock(item.getProduct().getId(), quantity);
                        }
                }

                order.setStatus(OrderStatus.CANCELED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                orderStatusHistoryRepository.save(OrderStatusHistory.systemChange(order, oldStatus, OrderStatus.CANCELED, reason));
                eventPublisher.publishEvent(new com.example.MyWeb.event.OrderStatusChangedEvent(this, order, oldStatus, OrderStatus.CANCELED));
                
                log.info("System cancelled order: orderId={}, reason={}", orderId, reason);
                try {
                        emailService.sendOrderStatusUpdate(order.getUser().getEmail(), orderId, "CANCELED (" + reason + ")");
                } catch (Exception e) {
                        log.error("Failed to send cancellation email for order {}", orderId);
                }
        }
}
