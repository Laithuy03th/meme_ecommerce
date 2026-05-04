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
        Page<Order> findByStatusOrderByCreatedAtDesc(com.example.MyWeb.model.enums.OrderStatus status,
                        Pageable pageable);

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

        Long countByUser_Id(Long userId);

        @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.user.id = :userId")
        Double sumTotalSpentByUserId(@Param("userId") Long userId);

        // Check if user has bought product with Delivered status
        boolean existsByUser_IdAndItems_Product_IdAndStatus(Long userId, Long productId,
                        com.example.MyWeb.model.enums.OrderStatus status);

        // Count voucher usage by user
        long countByUser_IdAndVoucherCode(Long userId, String voucherCode);

        /**
         * Idempotency check: tìm đơn hàng theo idempotencyKey và userId.
         * Nếu đã tồn tại → trả về đơn cũ, không tạo mới.
         */
        Optional<Order> findByIdempotencyKeyAndUser_Id(String idempotencyKey, Long userId);

        java.util.List<Order> findByStatusAndPaymentMethodAndPaymentStatusAndCreatedAtBefore(
                        com.example.MyWeb.model.enums.OrderStatus status,
                        com.example.MyWeb.model.enums.PaymentMethod paymentMethod,
                        com.example.MyWeb.model.enums.PaymentStatus paymentStatus,
                        java.time.LocalDateTime timeLimit);

        java.util.List<Order> findByStatusAndPaymentMethodAndPaymentStatusNotAndCreatedAtBefore(
                        com.example.MyWeb.model.enums.OrderStatus status,
                        com.example.MyWeb.model.enums.PaymentMethod paymentMethod,
                        com.example.MyWeb.model.enums.PaymentStatus paymentStatus,
                        java.time.LocalDateTime timeLimit);
}
