package com.example.MyWeb.controller;

import com.example.MyWeb.dto.cart.CartItemRequest;
import com.example.MyWeb.dto.cart.CartResponse;
import com.example.MyWeb.dto.cart.UpdateCartItemRequest;
import com.example.MyWeb.security.CustomUserDetails;
import com.example.MyWeb.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        return principal.getId();
    }

    // GET /api/v1/users/me/cart
    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(cartService.getCurrentCart(userId));
    }

    // POST /api/v1/users/me/cart/items
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody CartItemRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(cartService.addItem(userId, request));
    }

    // PUT /api/v1/users/me/cart/items/{itemId}
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(cartService.updateItem(userId, itemId, request));
    }

    // DELETE /api/v1/users/me/cart/items/{itemId}
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long itemId) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(cartService.removeItem(userId, itemId));
    }
}
