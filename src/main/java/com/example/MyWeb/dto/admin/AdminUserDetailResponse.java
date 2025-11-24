package com.example.MyWeb.dto.admin;

import com.example.MyWeb.dto.user.AddressResponse;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDetailResponse {

    private Long id;
    private String email;
    private String status;
    private Set<String> roles;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Profile
    private String fullName;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;

    // Address list
    private List<AddressResponse> addresses;

    // Order summaries
    private List<AdminOrderSummaryResponse> orders;
}
