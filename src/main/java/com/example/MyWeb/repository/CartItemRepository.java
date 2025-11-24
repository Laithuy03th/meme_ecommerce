package com.example.MyWeb.repository;

import com.example.MyWeb.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByIdAndCart_User_Id(Long id, Long userId);
}
