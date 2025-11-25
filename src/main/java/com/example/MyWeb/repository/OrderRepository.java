package com.example.MyWeb.repository;

import com.example.MyWeb.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Order> findByIdAndUser_Id(Long id, Long userId);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // cho admin – filter theo status
    Page<Order> findByStatusOrderByCreatedAtDesc(com.example.MyWeb.model.enums.OrderStatus status, Pageable pageable);

    // Dashboard statistics
    Long countByStatus(com.example.MyWeb.model.enums.OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status")
    Double sumTotalAmountByStatus(
            @Param("status") com.example.MyWeb.model.enums.OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status AND o.createdAt >= :startDate AND o.createdAt < :endDate")
    Double sumTotalAmountByStatusAndDateRange(
            @Param("status") com.example.MyWeb.model.enums.OrderStatus status,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);
}
