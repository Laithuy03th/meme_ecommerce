package com.example.MyWeb.service;

import com.example.MyWeb.dto.cart.CartItemRequest;
import com.example.MyWeb.dto.cart.CartResponse;
import com.example.MyWeb.dto.cart.UpdateCartItemRequest;

public interface CartService {

    CartResponse getCurrentCart(Long userId);

    CartResponse addItem(Long userId, CartItemRequest request);

    CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request);

    CartResponse removeItem(Long userId, Long itemId);
}
