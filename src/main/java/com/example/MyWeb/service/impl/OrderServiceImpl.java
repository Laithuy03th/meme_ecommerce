package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.order.CheckoutRequest;
import com.example.MyWeb.dto.order.OrderItemResponse;
import com.example.MyWeb.dto.order.OrderResponse;
import com.example.MyWeb.model.*;
import com.example.MyWeb.repository.*;
import com.example.MyWeb.service.OrderService;
import lombok.RequiredArgsConstructor;
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
public class OrderServiceImpl implements OrderService {

        private final CartRepository cartRepository;
        private final OrderRepository orderRepository;
        private final AddressRepository addressRepository;
        private final UserRepository userRepository;
        private final VoucherRepository voucherRepository;
        private final ProductRepository productRepository;
        private final ProductVariantRepository productVariantRepository;

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

                Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                                .orElseThrow(() -> new RuntimeException("Address not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                double itemsTotal = cart.getItems().stream()
                                .mapToDouble(CartItem::getTotalPrice)
                                .sum();

                double shippingFee = 0.0; // sau này có thể tính theo policy
                double discountAmount = 0.0;
                String voucherCode = null;

                // Apply voucher if provided
                if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
                        Voucher voucher = voucherRepository.findByCode(request.getVoucherCode().toUpperCase())
                                        .orElse(null);

                        if (voucher != null && voucher.isValid()) {
                                double subtotal = itemsTotal + shippingFee;
                                if (voucher.getMinOrderAmount() == null || subtotal >= voucher.getMinOrderAmount()) {
                                        discountAmount = voucher.calculateDiscount(subtotal);
                                        voucherCode = voucher.getCode();

                                        // Increment voucher usage
                                        voucher.setUsedCount(voucher.getUsedCount() + 1);
                                        voucherRepository.save(voucher);
                                }
                        }
                }

                double totalAmount = itemsTotal + shippingFee - discountAmount;

                LocalDateTime now = LocalDateTime.now();

                // Parse PaymentMethod from String
                PaymentMethod pm;
                try {
                        pm = PaymentMethod.valueOf(request.getPaymentMethod());
                } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid payment method: " + request.getPaymentMethod());
                }

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

                // map cart_items -> order_items (snapshot)
                for (CartItem ci : cart.getItems()) {
                        Product p = ci.getProduct();
                        ProductVariant v = ci.getVariant();
                        int qty = ci.getQuantity();

                        // Check & Deduct Stock
                        if (v != null) {
                                if (v.getStock() == null || v.getStock() < qty) {
                                        throw new RuntimeException("Out of stock for variant: " + p.getName() + " - "
                                                        + v.getColor() + "/" + v.getSize());
                                }
                                v.setStock(v.getStock() - qty);
                                productVariantRepository.save(v);
                        } else {
                                if (p.getStockQuantity() == null || p.getStockQuantity() < qty) {
                                        throw new RuntimeException("Out of stock for product: " + p.getName());
                                }
                                p.setStockQuantity(p.getStockQuantity() - qty);
                                productRepository.save(p);
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

                // Lưu order + items
                Order savedOrder = orderRepository.save(order);

                // Cart đã checkout → đổi status + clear items
                cart.setStatus(CartStatus.CHECKED_OUT);
                cart.getItems().clear();
                cart.setUpdatedAt(now);
                cartRepository.save(cart);

                return toOrderDto(savedOrder);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<OrderResponse> getMyOrders(Long userId, int page, int size) {
                Pageable pageable = PageRequest.of(page, size);
                Page<Order> orders = orderRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
                return orders.map(this::toOrderDto);
        }

        @Override
        @Transactional(readOnly = true)
        public OrderResponse getMyOrderDetail(Long userId, Long orderId) {
                Order order = orderRepository.findByIdAndUser_Id(orderId, userId)
                                .orElseThrow(() -> new RuntimeException("Order not found"));
                // ensure items loaded
                order.getItems().size();
                return toOrderDto(order);
        }

        @Override
        @Transactional
        public OrderResponse cancelOrder(Long userId, Long orderId) {
                Order order = orderRepository.findByIdAndUser_Id(orderId, userId)
                                .orElseThrow(() -> new RuntimeException("Order not found"));

                // Only allow cancel if order is PENDING
                if (order.getStatus() != OrderStatus.PENDING) {
                        throw new RuntimeException("Cannot cancel order with status: " + order.getStatus());
                }

                order.setStatus(OrderStatus.CANCELED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                return toOrderDto(order);
        }
}
