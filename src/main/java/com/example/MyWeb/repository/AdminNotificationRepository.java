package com.example.MyWeb.repository;

import com.example.MyWeb.model.AdminNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    
    Page<AdminNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByIsReadFalse();

    @Modifying
    @Query("UPDATE AdminNotification n SET n.isRead = true WHERE n.isRead = false")
    void markAllAsRead();
}
