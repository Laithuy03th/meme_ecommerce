// src/main/java/com/example/MyWeb/service/AdminOrderService.java
package com.example.MyWeb.service;

import com.example.MyWeb.dto.order.AdminOrderDetailResponse;
import com.example.MyWeb.dto.order.AdminOrderSummaryResponse;
import org.springframework.data.domain.Page;

public interface AdminOrderService {

    Page<AdminOrderSummaryResponse> list(String status, int page, int size);

    AdminOrderDetailResponse getDetail(Long orderId);

    AdminOrderDetailResponse updateStatus(Long orderId, String newStatus);
}
