package com.example.MyWeb.dto.user;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private Set<String> roles; // "ADMIN", "CUSTOMER"...
    private String avatarUrl;
    private String memberSince; // e.g. "2023"
    private Integer totalOrders;
    private Double totalSpent;
    private String membershipLevel; // e.g. "Gold"
    private Boolean verified;
}
