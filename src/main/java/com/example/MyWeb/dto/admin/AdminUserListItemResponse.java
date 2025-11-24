package com.example.MyWeb.dto.admin;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserListItemResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String status; // ACTIVE/BLOCKED
    private Set<String> roles; // ADMIN, CUSTOMER...
    private LocalDateTime createdAt;
}
