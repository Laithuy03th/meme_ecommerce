package com.example.MyWeb.repository;

import com.example.MyWeb.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser_IdAndStatus(Long userId, String status);
}
