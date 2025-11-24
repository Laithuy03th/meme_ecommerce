package com.example.MyWeb.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class AdminCreateUserRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String fullName;
    private String phone;

    /**
     * Danh sách role code, ví dụ: ["ADMIN"], ["CUSTOMER"], hoặc cả hai.
     * Lưu ý khi map sang GrantedAuthority phải prefix ROLE_*
     */
    @NotBlank
    private Set<String> roles;
}
