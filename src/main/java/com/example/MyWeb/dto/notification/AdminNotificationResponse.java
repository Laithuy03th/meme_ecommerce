package com.example.MyWeb.dto.notification;

import com.example.MyWeb.model.enums.AdminNotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationResponse {
    private Long id;
    private String title;
    private String message;
    private AdminNotificationType type;
    private String targetType;
    private Long targetId;
    private String actionUrl;
    private String createdBy;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
