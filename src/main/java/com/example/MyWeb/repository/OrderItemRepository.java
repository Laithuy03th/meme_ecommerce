package com.example.MyWeb.repository;

import com.example.MyWeb.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

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
}
