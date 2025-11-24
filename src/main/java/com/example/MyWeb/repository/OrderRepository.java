package com.example.MyWeb.repository;

import com.example.MyWeb.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Order> findByIdAndUser_Id(Long id, Long userId);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // cho admin – filter theo status
    Page<Order> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
