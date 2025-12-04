package com.example.MyWeb.repository;

import com.example.MyWeb.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByOrder_IdOrderByCreatedAtDesc(Long orderId);

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    Optional<PaymentTransaction> findFirstByOrder_IdOrderByCreatedAtDesc(Long orderId);
}
