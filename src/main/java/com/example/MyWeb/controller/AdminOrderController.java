// src/main/java/com/example/MyWeb/controller/AdminOrderController.java
package com.example.MyWeb.controller;

import com.example.MyWeb.dto.order.AdminOrderDetailResponse;
import com.example.MyWeb.dto.order.AdminOrderSummaryResponse;
import com.example.MyWeb.dto.order.UpdateOrderStatusRequest;
import com.example.MyWeb.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<Page<AdminOrderSummaryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminOrderService.list(status, page, size));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderDetailResponse> detail(@PathVariable Long orderId) {
        return ResponseEntity.ok(adminOrderService.getDetail(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<AdminOrderDetailResponse> updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(adminOrderService.updateStatus(orderId, request.getStatus()));
    }
}
