package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.notification.AdminNotificationResponse;
import com.example.MyWeb.model.AdminNotification;
import com.example.MyWeb.model.enums.AdminNotificationType;
import com.example.MyWeb.repository.AdminNotificationRepository;
import com.example.MyWeb.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final AdminNotificationRepository adminNotificationRepository;

    @Override
    @Transactional
    public void createNotification(String title, String message, AdminNotificationType type, String targetType, Long targetId, String actionUrl, String createdBy) {
        try {
            AdminNotification notification = AdminNotification.builder()
                    .title(title)
                    .message(message)
                    .type(type)
                    .targetType(targetType)
                    .targetId(targetId)
                    .actionUrl(actionUrl)
                    .createdBy(createdBy)
                    .isRead(false)
                    .build();
            adminNotificationRepository.save(notification);
            log.info("Created Admin Notification: {}", title);
        } catch (Exception e) {
            log.error("Failed to create admin notification", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminNotificationResponse> getNotifications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return adminNotificationRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return adminNotificationRepository.countByIsReadFalse();
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        adminNotificationRepository.findById(id).ifPresent(notification -> {
            notification.setIsRead(true);
            adminNotificationRepository.save(notification);
        });
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        adminNotificationRepository.markAllAsRead();
    }

    private AdminNotificationResponse toResponse(AdminNotification notification) {
        return AdminNotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .actionUrl(notification.getActionUrl())
                .createdBy(notification.getCreatedBy())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
