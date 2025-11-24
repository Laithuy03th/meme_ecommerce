package com.example.MyWeb.controller;

import com.example.MyWeb.dto.admin.AdminCreateUserRequest;
import com.example.MyWeb.dto.admin.AdminUpdateUserRolesRequest;
import com.example.MyWeb.dto.admin.AdminResetPasswordRequest;
import com.example.MyWeb.dto.admin.AdminUserDetailResponse;
import com.example.MyWeb.dto.admin.AdminUserListItemResponse;
import com.example.MyWeb.dto.admin.UpdateUserStatusRequest;
import com.example.MyWeb.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * GET /api/v1/admin/users?status=ACTIVE&role=CUSTOMER&page=0&size=20
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AdminUserListItemResponse>> listUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AdminUserListItemResponse> res = adminUserService.listUsers(role, status, page, size);
        return ResponseEntity.ok(res);
    }

    /**
     * GET /api/v1/admin/users/{userId}
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserDetailResponse> getUserDetail(
            @PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.getUserDetail(userId));
    }

    /**
     * PATCH /api/v1/admin/users/{userId}/status
     * Body: { "status": "BLOCKED" }
     */
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserDetailResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        return ResponseEntity.ok(adminUserService.updateUserStatus(userId, request));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserDetailResponse> createUser(
            @Valid @RequestBody AdminCreateUserRequest req) {
        return ResponseEntity.ok(adminUserService.createUser(req));
    }

    @PatchMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserDetailResponse> replaceRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRolesRequest req) {
        return ResponseEntity.ok(adminUserService.replaceRoles(userId, req));
    }

    @PatchMapping("/{userId}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody AdminResetPasswordRequest req) {
        adminUserService.resetPassword(userId, req);
        return ResponseEntity.noContent().build();
    }

}
