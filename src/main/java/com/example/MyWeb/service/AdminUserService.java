package com.example.MyWeb.service;

import com.example.MyWeb.dto.admin.AdminCreateUserRequest;
import com.example.MyWeb.dto.admin.AdminUpdateUserRolesRequest;
import com.example.MyWeb.dto.admin.AdminResetPasswordRequest;
import com.example.MyWeb.dto.admin.AdminUserDetailResponse;
import com.example.MyWeb.dto.admin.AdminUserListItemResponse;
import com.example.MyWeb.dto.admin.UpdateUserStatusRequest;
import org.springframework.data.domain.Page;

public interface AdminUserService {

    Page<AdminUserListItemResponse> listUsers(String role, String status, int page, int size);

    AdminUserDetailResponse getUserDetail(Long userId);

    AdminUserDetailResponse updateUserStatus(Long userId, UpdateUserStatusRequest request);

    AdminUserDetailResponse createUser(AdminCreateUserRequest req);

    AdminUserDetailResponse replaceRoles(Long userId, AdminUpdateUserRolesRequest req);

    void resetPassword(Long userId, AdminResetPasswordRequest req);

}
