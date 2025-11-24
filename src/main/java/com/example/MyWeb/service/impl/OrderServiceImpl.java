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

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

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
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
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

        Cart cart = cartRepository.findByUser_IdAndStatus(userId, "ACTIVE")
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
        double totalAmount = itemsTotal + shippingFee;

        LocalDateTime now = LocalDateTime.now();

        Order order = Order.builder()
                .user(user)
                .address(address)
                .status("PENDING") // sau này có thể update thành PAID...
                .paymentStatus("UNPAID") // vì mới checkout
                .paymentMethod(request.getPaymentMethod())
                .totalAmount(totalAmount)
                .shippingFee(shippingFee)
                .note(request.getNote())
                .createdAt(now)
                .updatedAt(now)
                .items(new ArrayList<>())
                .build();

        // map cart_items -> order_items (snapshot)
        for (CartItem ci : cart.getItems()) {
            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .product(ci.getProduct())
                    .variant(ci.getVariant())
                    .productName(ci.getProduct().getName())
                    .unitPrice(ci.getUnitPrice())
                    .quantity(ci.getQuantity())
                    .totalPrice(ci.getTotalPrice())
                    .createdAt(now)
                    .build();
            order.getItems().add(oi);
        }

        // Lưu order + items
        Order savedOrder = orderRepository.save(order);

        // Cart đã checkout → đổi status + clear items
        cart.setStatus("CHECKED_OUT");
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
}
