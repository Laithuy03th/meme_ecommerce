// src/main/java/com/example/MyWeb/controller/AdminOrderController.java
package com.example.MyWeb.controller;

import com.example.MyWeb.dto.order.AdminOrderDetailResponse;
import com.example.MyWeb.dto.order.AdminOrderSummaryResponse;
import com.example.MyWeb.dto.order.OrderResponse;
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
    private final com.example.MyWeb.service.OrderService orderService; // Inject OrderService for return logic

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

    // Updated to match Swagger (PUT) and support new simple status update
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        // Use AdminOrderService if it has logic, or OrderService.
        // Let's stick to AdminOrderService for consistency if possible, but fallback to
        // OrderService for simple status update if needed.
        // Existing code used adminOrderService.updateStatus
        return ResponseEntity.ok(adminOrderService.updateStatus(orderId, request.getStatus()));
    }

    // New endpoint from the deleted controller
    @PutMapping("/{orderId}/return/approve")
    public ResponseEntity<?> approveReturn(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.approveReturn(orderId));
    }

    // Chuyển RETURNED → REFUNDED sau khi hoàn tiền cho khách
    @PutMapping("/{orderId}/refund")
    public ResponseEntity<?> refundOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(adminOrderService.updateStatus(orderId, "REFUNDED"));
    }
}
