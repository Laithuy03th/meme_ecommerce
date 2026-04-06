// src/main/java/com/example/MyWeb/service/impl/AdminOrderServiceImpl.java
package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.order.AdminOrderDetailResponse;
import com.example.MyWeb.dto.order.AdminOrderItemResponse;
import com.example.MyWeb.dto.order.AdminOrderSummaryResponse;
import com.example.MyWeb.model.Address;
import com.example.MyWeb.model.Order;
import com.example.MyWeb.model.OrderItem;
import com.example.MyWeb.repository.OrderRepository;
import com.example.MyWeb.service.AdminOrderService;
import com.example.MyWeb.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.example.MyWeb.model.enums.OrderStatus;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    @Lazy
    private final OrderService orderService; // Delegate state machine & stock logic

    private AdminOrderSummaryResponse toSummaryDto(Order o) {
        Address addr = o.getAddress();

        String shippingFullName = null;
        if (addr != null && addr.getFullName() != null) {
            shippingFullName = addr.getFullName();
        }

        return AdminOrderSummaryResponse.builder()
                .id(o.getId())
                .userEmail(o.getUser().getEmail())
                .shippingFullName(shippingFullName)
                .totalAmount(o.getTotalAmount())
                .shippingFee(o.getShippingFee())
                .status(o.getStatus().name())
                .paymentMethod(o.getPaymentMethod().name())
                .paymentStatus(o.getPaymentStatus().name())
                .createdAt(o.getCreatedAt())
                .build();
    }

    private AdminOrderItemResponse toItemDto(OrderItem item) {
        return AdminOrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .lineTotal(item.getTotalPrice()) // hoặc getLineTotal() nếu bạn đặt tên khác
                .build();
    }

    private AdminOrderDetailResponse toDetailDto(Order o) {
        Address addr = o.getAddress();

        String fullName = null;
        String phone = null;
        String addrLine1 = null;
        String ward = null;
        String district = null;
        String province = null;
        String country = null;

        if (addr != null) {
            fullName = addr.getFullName();
            phone = addr.getPhone();
            addrLine1 = addr.getAddressLine1();
            ward = addr.getWard();
            district = addr.getDistrict();
            province = addr.getProvince();
            country = addr.getCountry();
        }

        List<AdminOrderItemResponse> itemDtos = o.getItems()
                .stream()
                .map(this::toItemDto)
                .toList();

        return AdminOrderDetailResponse.builder()
                .id(o.getId())
                .status(o.getStatus().name())
                .paymentMethod(o.getPaymentMethod().name())
                .paymentStatus(o.getPaymentStatus().name())
                .totalAmount(o.getTotalAmount())
                .shippingFee(o.getShippingFee())
                .note(o.getNote())
                .userId(o.getUser().getId())
                .userEmail(o.getUser().getEmail())
                .shippingFullName(fullName)
                .shippingPhone(phone)
                .shippingAddressLine1(addrLine1)
                .shippingWard(ward)
                .shippingDistrict(district)
                .shippingProvince(province)
                .shippingCountry(country)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .items(itemDtos)
                .build();
    }

    @Override
    public Page<AdminOrderSummaryResponse> list(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> orderPage;
        if (status != null && !status.isBlank()) {
            try {
                OrderStatus os = OrderStatus.valueOf(status.toUpperCase());
                orderPage = orderRepository.findByStatusOrderByCreatedAtDesc(os, pageable);
            } catch (IllegalArgumentException e) {
                // Nếu status không hợp lệ, có thể trả về empty hoặc throw error.
                // Ở đây mình chọn trả về empty page cho an toàn
                orderPage = Page.empty(pageable);
            }
        } else {
            orderPage = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return orderPage.map(this::toSummaryDto);
    }

    @Override
    public AdminOrderDetailResponse getDetail(Long orderId) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toDetailDto(o);
    }

    @Override
    @Transactional
    public AdminOrderDetailResponse updateStatus(Long orderId, String newStatus) {
        // Delegate xuống OrderService — nơi chứa State Machine và logic hoàn stock
        // Tránh trùng lặp code và đảm bảo nhất quán (L5/L13 fix)
        orderService.updateOrderStatus(orderId, newStatus);

        // Reload để trả về AdminOrderDetailResponse mới nhất
        return getDetail(orderId);
    }
}
