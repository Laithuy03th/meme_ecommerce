package com.example.MyWeb.service;

import com.example.MyWeb.dto.order.CheckoutRequest;
import com.example.MyWeb.dto.order.OrderResponse;
import org.springframework.data.domain.Page;

public interface OrderService {

    OrderResponse checkout(Long userId, CheckoutRequest request);

    Page<com.example.MyWeb.dto.order.OrderListResponse> getMyOrders(Long userId, int page, int size);

    com.example.MyWeb.dto.order.OrderDetailResponse getMyOrderDetail(Long userId, Long orderId);

    OrderResponse cancelOrder(Long userId, Long orderId);

    OrderResponse requestReturn(Long userId, Long orderId, String reason);

    /**
     * Re-order: Add all items from an old order to cart
     * Shopee-level feature for quick re-purchase
     */
    com.example.MyWeb.dto.cart.CartResponse reorder(Long userId, Long orderId);

    // Admin methods
    OrderResponse approveReturn(Long orderId);

    OrderResponse updateOrderStatus(Long orderId, String newStatus);

    void systemCancelOrder(Long orderId, String reason);
}
