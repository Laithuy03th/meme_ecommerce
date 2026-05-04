package com.example.MyWeb.service;

import com.example.MyWeb.dto.notification.NotificationResponse;
import com.example.MyWeb.model.User;
import org.springframework.data.domain.Page;

public interface NotificationService {

    void createNotification(User user, String title, String message, String type, Long relatedId);

    Page<NotificationResponse> getUserNotifications(Long userId, int page, int size);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}
