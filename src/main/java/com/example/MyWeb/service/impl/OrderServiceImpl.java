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

                // IMPROVED: Validate ALL stock BEFORE creating order (Only for checkout items)
                for (CartItem ci : checkoutItems) {
                        Product p = ci.getProduct();
                        ProductVariant v = ci.getVariant();
                        int qty = ci.getQuantity();

                        if (v != null) {
                                // Validate variant stock
                                Integer variantStock = v.getStock() != null ? v.getStock() : 0;
                                if (variantStock < qty) {
                                        throw new RuntimeException("Out of stock for variant: " + p.getName() + " - "
                                                        + v.getColor() + "/" + v.getSize() + " (Available: "
                                                        + variantStock + ", Requested: " + qty + ")");
                                }
                        } else {
                                // Validate product stock using StockService
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

                                // Increment voucher usage
                                voucher.setUsedCount(voucher.getUsedCount() + 1);
                                voucherRepository.save(voucher);
                        } else {
                                // If eligibleAmount is 0 but we are here, it means voucher valid but no product
                                // match.
                                // If it gave free shipping, we should still record usage and save voucherCode?
                                // Yes, if Free Shipping was applied.
                                if (Boolean.TRUE.equals(voucher.getFreeShipping())) {
                                        voucherCode = voucher.getCode();
                                        voucher.setUsedCount(voucher.getUsedCount() + 1);
                                        voucherRepository.save(voucher);
                                } else {
                                        throw new RuntimeException(
                                                        "This voucher is not applicable to any items in your cart");
                                }
                        }
                }

                double totalAmount = itemsTotal + shippingFee - discountAmount;

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
                                .createdAt(now)
                                .updatedAt(now)
                                .items(new ArrayList<>())
                                .build();

                // map cart_items -> order_items (snapshot) & decrease stock
                for (CartItem ci : checkoutItems) {
                        Product p = ci.getProduct();
                        ProductVariant v = ci.getVariant();
                        int qty = ci.getQuantity();

                        // IMPROVED: Use StockService for proper stock management
                        if (v != null) {
                                // Handle variant stock decrease (separate logic for variants)
                                if (v.getStock() == null || v.getStock() < qty) {
                                        throw new RuntimeException("Out of stock for variant: " + p.getName() + " - "
                                                        + v.getColor() + "/" + v.getSize());
                                }
                                v.setStock(v.getStock() - qty);
                                productVariantRepository.save(v);

                                log.info("Decreased variant stock: product={}, variant={}, quantity={}",
                                                p.getId(), v.getId(), qty);
                        } else {
                                // Use StockService for product-level stock
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

                // FIXED: Partial Checkout - Only remove purchased items
                cart.getItems().removeAll(checkoutItems);

                // Also delete from DB (orphanRemoval=true in Entity ensures this, but explicit
                // delete is safer for many-to-many logic if cascade fails)
                // With CascadeType.ALL + orphanRemoval=true, removing from list should be
                // enough if we save cart.
                // However, to be 100% sure we don't have dangling items:
                // (Hibernate handles this if mapped correctly)

                if (cart.getItems().isEmpty()) {
                        // Option: Set to CHECKED_OUT or Keep ACTIVE?
                        // If we keep ACTIVE, user can continue adding items easily.
                        // Let's keep it ACTIVE for better partial checkout UX globally.
                        // But if business logic requires new cart per session, we can close it.
                        // For now: Keep ACTIVE.
                        // cart.setStatus(CartStatus.CHECKED_OUT);
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
                if (order.getItems() != null && !order.getItems().isEmpty()) {
                        firstImage = order.getItems().get(0).getProduct().getThumbnailUrl();
                }

                return com.example.MyWeb.dto.order.OrderListResponse.builder()
                                .id(order.getId())
                                .orderNumber("ORD-" + order.getId())
                                .createdAt(order.getCreatedAt())
                                .status(order.getStatus().name())
                                .itemCount(order.getItems() != null ? order.getItems().size() : 0)
                                .totalAmount(order.getTotalAmount())
                                .firstItemImageUrl(firstImage)
                                .build();
        }

        private com.example.MyWeb.dto.order.OrderDetailResponse toDetailResponse(Order order) {
                java.util.List<com.example.MyWeb.dto.order.OrderDetailResponse.TimelineStep> timeline = new ArrayList<>();
                timeline.add(com.example.MyWeb.dto.order.OrderDetailResponse.TimelineStep.builder()
                                .status("Order Placed")
                                .timestamp(order.getCreatedAt())
                                .completed(true)
                                .build());

                java.util.List<com.example.MyWeb.dto.order.OrderDetailResponse.OrderItemDto> items = order.getItems()
                                .stream()
                                .map(item -> com.example.MyWeb.dto.order.OrderDetailResponse.OrderItemDto.builder()
                                                .id(item.getId())
                                                .productName(item.getProductName())
                                                .productImageUrl(item.getProduct().getThumbnailUrl())
                                                .variantInfo(item.getVariant() != null
                                                                ? "Color: " + item.getVariant().getColor() + ", Size: "
                                                                                + item.getVariant().getSize()
                                                                : "")
                                                .quantity(item.getQuantity())
                                                .price(item.getUnitPrice())
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

                order.setStatus(OrderStatus.CANCELED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

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

                order.setStatus(OrderStatus.RETURN_REQUESTED);
                // Note: We might want to append the reason to the order note or a separate
                // field
                // For now, appending to note
                String currentNote = order.getNote() != null ? order.getNote() : "";
                order.setNote(currentNote + " [Return Reason: " + reason + "]");

                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

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

                order.setStatus(OrderStatus.RETURNED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                log.info("Order return approved and stock returned: orderId={}", orderId);
                return toOrderDto(order);
        }

        @Override
        @Transactional
        public OrderResponse updateOrderStatus(Long orderId, String newStatus) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found"));

                OrderStatus status;
                try {
                        status = OrderStatus.valueOf(newStatus);
                } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid order status: " + newStatus);
                }

                // If status is CANCELLED or RETURNED from admin panel, we should handle stock
                // return
                // Reuse cancel/return logic?
                // Best practice: if admin sets CANCELLED, treat it as cancelling order.
                if (status == OrderStatus.CANCELED) {
                        if (order.getStatus() != OrderStatus.CANCELED && order.getStatus() != OrderStatus.RETURNED) {
                                // Only return stock if not already cancelled/returned
                                return cancelOrder(order.getUser().getId(), orderId); // Admin acts on behalf of user?
                                // Check cancelOrder impl: it requires userId and checks ownership?
                                // Our cancelOrder checks: findByIdAndUser_Id -> This will fail if we pass admin
                                // id or random id.
                                // So we must duplicate stock return logic here or refactor.
                                // Let's duplicate tiny logic for safety and speed.

                                // Logic below...
                        }
                }

                // If Admin manually sets status, we assume they know what they are doing.
                // Exception: CANCELED/RETURNED should return stock.
                if ((status == OrderStatus.CANCELED || status == OrderStatus.RETURNED) &&
                                (order.getStatus() != OrderStatus.CANCELED
                                                && order.getStatus() != OrderStatus.RETURNED)) {

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
                }

                order.setStatus(status);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                return toOrderDto(order);
        }
}
