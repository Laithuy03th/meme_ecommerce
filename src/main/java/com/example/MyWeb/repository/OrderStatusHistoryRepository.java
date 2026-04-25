package com.example.MyWeb.repository;

import com.example.MyWeb.model.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrder_IdOrderByCreatedAtAsc(Long orderId);
    
    java.util.Optional<OrderStatusHistory> findFirstByOrder_IdAndToStatusOrderByCreatedAtDesc(Long orderId, com.example.MyWeb.model.enums.OrderStatus status);


}
