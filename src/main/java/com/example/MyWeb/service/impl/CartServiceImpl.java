package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.cart.CartItemRequest;
import com.example.MyWeb.dto.cart.CartItemResponse;
import com.example.MyWeb.dto.cart.CartResponse;
import com.example.MyWeb.dto.cart.UpdateCartItemRequest;
import com.example.MyWeb.model.*;
import com.example.MyWeb.repository.*;
import com.example.MyWeb.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    private CartItemResponse toItemDto(CartItem item) {
        Product p = item.getProduct();
        ProductVariant v = item.getVariant();

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(p.getId())
                .variantId(v != null ? v.getId() : null)
                .productName(p.getName())
                .productSlug(p.getSlug())
                .thumbnailUrl(p.getThumbnailUrl())
                .color(v != null ? v.getColor() : null)
                .size(v != null ? v.getSize() : null)
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .totalPrice(item.getTotalPrice())
                .build();
    }

    private CartResponse toCartDto(Cart cart) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return CartResponse.builder()
                    .id(cart != null ? cart.getId() : null)
                    .totalAmount(0.0)
                    .totalItems(0)
                    .items(new ArrayList<>())
                    .build();
        }

        var items = cart.getItems().stream()
                .map(this::toItemDto)
                .toList();

        double totalAmount = items.stream()
                .mapToDouble(CartItemResponse::getTotalPrice)
                .sum();

        int totalItems = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .items(items)
                .build();
    }

    private Cart getOrCreateActiveCart(Long userId) {
        return cartRepository.findByUser_IdAndStatus(userId, "ACTIVE")
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    Cart newCart = Cart.builder()
                            .user(user)
                            .status("ACTIVE")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .items(new ArrayList<>())
                            .build();

                    return cartRepository.save(newCart);
                });
    }

    private double resolvePrice(Product product, ProductVariant variant) {
        if (variant != null && variant.getPrice() != null) {
            return variant.getPrice();
        }
        return product.getBasePrice();
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCurrentCart(Long userId) {
        Cart cart = cartRepository.findByUser_IdAndStatus(userId, "ACTIVE")
                .orElse(null);
        if (cart == null) {
            return CartResponse.builder()
                    .id(null)
                    .totalAmount(0.0)
                    .totalItems(0)
                    .items(new ArrayList<>())
                    .build();
        }
        // ensure items loaded
        cart.getItems().size();
        return toCartDto(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        Cart cart = getOrCreateActiveCart(userId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Variant not found"));
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new RuntimeException("Variant does not belong to product");
            }
        }

        // tìm item cùng product + variant trong cart (nếu có thì tăng số lượng)
        CartItem existing = null;
        if (cart.getItems() != null) {
            for (CartItem ci : cart.getItems()) {
                Long vId = (ci.getVariant() != null ? ci.getVariant().getId() : null);
                if (ci.getProduct().getId().equals(product.getId())
                        && ((vId == null && request.getVariantId() == null)
                                || (vId != null && vId.equals(request.getVariantId())))) {
                    existing = ci;
                    break;
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        double unitPrice = resolvePrice(product, variant);

        if (existing != null) {
            int newQty = existing.getQuantity() + request.getQuantity();
            existing.setQuantity(newQty);
            existing.setUnitPrice(unitPrice);
            existing.setTotalPrice(unitPrice * newQty);
            existing.setUpdatedAt(now);
            cartItemRepository.save(existing);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(unitPrice * request.getQuantity())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        cart.setUpdatedAt(now);
        cartRepository.save(cart);

        return getCurrentCart(userId);
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findByIdAndCart_User_Id(itemId, userId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        int newQty = request.getQuantity();
        if (newQty <= 0) {
            // nếu muốn, có thể xoá luôn item
            cartItemRepository.delete(item);
        } else {
            double unitPrice = item.getUnitPrice();
            item.setQuantity(newQty);
            item.setTotalPrice(unitPrice * newQty);
            item.setUpdatedAt(LocalDateTime.now());
            cartItemRepository.save(item);
        }

        return getCurrentCart(userId);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCart_User_Id(itemId, userId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        Cart cart = item.getCart();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return getCurrentCart(userId);
    }
}
