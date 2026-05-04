package com.example.MyWeb.dto.notification;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private Long relatedId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
