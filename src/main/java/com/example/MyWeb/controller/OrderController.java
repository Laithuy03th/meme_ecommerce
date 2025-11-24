package com.example.MyWeb.controller;

import com.example.MyWeb.dto.order.CheckoutRequest;
import com.example.MyWeb.dto.order.OrderResponse;
import com.example.MyWeb.security.CustomUserDetails;
import com.example.MyWeb.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        return principal.getId();
    }

    // POST /api/v1/users/me/orders/checkout
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(orderService.checkout(userId, request));
    }

    // GET /api/v1/users/me/orders?page=0&size=10
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Page<OrderResponse> res = orderService.getMyOrders(userId, page, size);
        return ResponseEntity.ok(res);
    }

    // GET /api/v1/users/me/orders/{orderId}
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(orderService.getMyOrderDetail(userId, orderId));
    }
}
