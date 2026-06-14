package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.wishlist.WishlistResponse;
import com.example.MyWeb.exception.ResourceNotFoundException;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.model.User;
import com.example.MyWeb.model.Wishlist;
import com.example.MyWeb.repository.ProductRepository;
import com.example.MyWeb.repository.UserRepository;
import com.example.MyWeb.repository.WishlistRepository;
import com.example.MyWeb.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist(Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        return wishlists.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public WishlistResponse addToWishlist(Long userId, Long productId) {

        if (wishlistRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            throw new RuntimeException("Product already in wishlist");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        wishlist = wishlistRepository.save(wishlist);
        return toDto(wishlist);
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        wishlistRepository.deleteByUser_IdAndProduct_Id(userId, productId);
    }

    private WishlistResponse toDto(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .thumbnailUrl(product.getThumbnailUrl())
                .basePrice(product.getBasePrice())
                .createdAt(wishlist.getCreatedAt())
                .build();
    }
}
