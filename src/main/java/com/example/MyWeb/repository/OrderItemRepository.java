package com.example.MyWeb.repository;

import com.example.MyWeb.model.OrderItem;
import com.example.MyWeb.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

        @Query("SELECT oi.product.id as productId, oi.product.name as productName, " +
                        "oi.product.slug as productSlug, SUM(oi.quantity) as totalSold, " +
                        "SUM(oi.totalPrice) as totalRevenue " +
                        "FROM OrderItem oi " +
                        "WHERE oi.order.status = 'DELIVERED' " +
                        "GROUP BY oi.product.id, oi.product.name, oi.product.slug " +
                        "ORDER BY totalSold DESC " +
                        "LIMIT 10")
        List<Object[]> findTopSellingProducts();

        @Query("SELECT oi FROM OrderItem oi " +
                        "WHERE oi.order.user.id = :userId " +
                        "AND oi.product.id = :productId " +
                        "AND oi.order.status = :status " +
                        "ORDER BY oi.order.createdAt DESC")
        List<OrderItem> findByUserIdAndProductIdAndOrderStatus(
                        @Param("userId") Long userId,
                        @Param("productId") Long productId,
                        @Param("status") OrderStatus status);

        @Query("SELECT oi FROM OrderItem oi " +
                        "WHERE oi.id = :orderItemId " +
                        "AND oi.order.user.id = :userId " +
                        "AND oi.order.status = 'DELIVERED'")
        Optional<OrderItem> findReviewableOrderItem(
                        @Param("orderItemId") Long orderItemId,
                        @Param("userId") Long userId);
}
