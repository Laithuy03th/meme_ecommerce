package com.example.MyWeb.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class AdminUpdateUserRolesRequest {
    @NotEmpty
    private Set<String> roles; // ví dụ: ["CUSTOMER"], ["ADMIN", "CUSTOMER"]
}
