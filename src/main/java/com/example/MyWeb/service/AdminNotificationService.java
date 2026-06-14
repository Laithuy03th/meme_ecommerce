package com.example.MyWeb.service;

import com.example.MyWeb.dto.notification.AdminNotificationResponse;
import com.example.MyWeb.model.enums.AdminNotificationType;
import org.springframework.data.domain.Page;

public interface AdminNotificationService {
    void createNotification(String title, String message, AdminNotificationType type, String targetType, Long targetId, String actionUrl, String createdBy);
    Page<AdminNotificationResponse> getNotifications(int page, int size);
    long getUnreadCount();
    void markAsRead(Long id);
    void markAllAsRead();
}
