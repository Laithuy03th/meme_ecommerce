package com.example.MyWeb.service;

import com.example.MyWeb.dto.order.CheckoutRequest;
import com.example.MyWeb.dto.order.OrderResponse;
import org.springframework.data.domain.Page;

public interface OrderService {

    OrderResponse checkout(Long userId, CheckoutRequest request);

    Page<OrderResponse> getMyOrders(Long userId, int page, int size);

    OrderResponse getMyOrderDetail(Long userId, Long orderId);
}
