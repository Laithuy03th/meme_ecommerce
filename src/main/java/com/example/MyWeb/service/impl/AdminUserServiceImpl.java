package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.admin.AdminCreateUserRequest;
import com.example.MyWeb.dto.admin.AdminOrderSummaryResponse;
import com.example.MyWeb.dto.admin.AdminResetPasswordRequest;
import com.example.MyWeb.dto.admin.AdminUpdateUserRolesRequest;
import com.example.MyWeb.dto.admin.AdminUserDetailResponse;
import com.example.MyWeb.dto.admin.AdminUserListItemResponse;
import com.example.MyWeb.dto.admin.UpdateUserStatusRequest;
import com.example.MyWeb.dto.user.AddressResponse;
import com.example.MyWeb.model.CustomerProfile;
import com.example.MyWeb.model.Order;
import com.example.MyWeb.model.Role;
import com.example.MyWeb.model.User;
import com.example.MyWeb.repository.CustomerProfileRepository;
import com.example.MyWeb.repository.OrderRepository;
import com.example.MyWeb.repository.RoleRepository;
import com.example.MyWeb.repository.UserRepository;
import com.example.MyWeb.service.AddressService;
import com.example.MyWeb.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.MyWeb.model.enums.UserStatus;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

        private final UserRepository userRepository;
        private final CustomerProfileRepository profileRepository;
        private final OrderRepository orderRepository;
        private final AddressService addressService;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public Page<AdminUserListItemResponse> listUsers(String role, String status, int page, int size) {

                Pageable pageable = PageRequest.of(page, size);

                // Chuẩn hoá tham số
                String normalizedStatus = (status != null && !status.isBlank())
                                ? status.trim().toUpperCase()
                                : null;
                String normalizedRole = (role != null && !role.isBlank())
                                ? role.trim().toUpperCase()
                                : null;

                Page<User> userPage;

                if (normalizedStatus != null && normalizedRole != null) {
                        userPage = userRepository
                                        .findDistinctByStatusAndRoles_CodeOrderByCreatedAtDesc(
                                                        normalizedStatus, normalizedRole, pageable);
                } else if (normalizedStatus != null) {
                        userPage = userRepository
                                        .findByStatusOrderByCreatedAtDesc(normalizedStatus, pageable);
                } else if (normalizedRole != null) {
                        userPage = userRepository
                                        .findDistinctByRoles_CodeOrderByCreatedAtDesc(normalizedRole, pageable);
                } else {
                        userPage = userRepository
                                        .findAllByOrderByCreatedAtDesc(pageable);
                }

                // Map sang DTO
                return userPage.map(this::toListItemDto);
        }

        private AdminUserListItemResponse toListItemDto(User user) {

                CustomerProfile profile = profileRepository.findByUser(user).orElse(null);

                Set<String> roleCodes = user.getRoles()
                                .stream()
                                .map(Role::getCode)
                                .collect(Collectors.toSet());

                return AdminUserListItemResponse.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .status(user.getStatus().name())
                                .roles(roleCodes)
                                .createdAt(user.getCreatedAt())
                                .fullName(profile != null ? profile.getFullName() : null)
                                .phone(profile != null ? profile.getPhone() : null)
                                .build();
        }

        @Override
        public AdminUserDetailResponse getUserDetail(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return buildDetailDto(user);
        }

        @Override
        @Transactional
        public AdminUserDetailResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                String status = request.getStatus();
                if (status == null || status.isBlank()) {
                        throw new RuntimeException("Status is required");
                }

                String normalized = status.trim().toUpperCase();
                UserStatus us;
                try {
                        us = UserStatus.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid status: " + status);
                }

                user.setStatus(us);
                userRepository.save(user);

                return buildDetailDto(user);
        }

        private AdminUserDetailResponse buildDetailDto(User user) {

                CustomerProfile profile = profileRepository.findByUser(user).orElse(null);

                Set<String> roleCodes = user.getRoles()
                                .stream()
                                .map(Role::getCode)
                                .collect(Collectors.toSet());

                // Lấy địa chỉ (reuse AddressService -> AddressResponse)
                List<AddressResponse> addresses = addressService.list(user.getId());

                // Lấy danh sách orders mới nhất (ví dụ lấy 20 đơn gần nhất)
                List<AdminOrderSummaryResponse> orders = orderRepository
                                .findByUser_IdOrderByCreatedAtDesc(user.getId(),
                                                PageRequest.of(0, 20))
                                .getContent()
                                .stream()
                                .map(this::toOrderSummaryDto)
                                .toList();

                return AdminUserDetailResponse.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .status(user.getStatus().name())
                                .roles(roleCodes)
                                .createdAt(user.getCreatedAt())
                                .updatedAt(user.getUpdatedAt())

                                .fullName(profile != null ? profile.getFullName() : null)
                                .phone(profile != null ? profile.getPhone() : null)
                                .gender(profile != null ? profile.getGender() : null)
                                .dateOfBirth(profile != null ? profile.getDateOfBirth() : null)

                                .addresses(addresses)
                                .orders(orders)
                                .build();
        }

        private AdminOrderSummaryResponse toOrderSummaryDto(Order order) {
                return AdminOrderSummaryResponse.builder()
                                .id(order.getId())
                                .status(order.getStatus().name())
                                .paymentStatus(order.getPaymentStatus().name())
                                .totalAmount(order.getTotalAmount())
                                .shippingFee(order.getShippingFee())
                                .createdAt(order.getCreatedAt())
                                .build();
        }

        @Override
        @Transactional
        public AdminUserDetailResponse createUser(AdminCreateUserRequest req) {
                if (userRepository.existsByEmail(req.getEmail())) {
                        throw new RuntimeException("Email already exists");
                }
                var roles = req.getRoles().stream()
                                .map(code -> roleRepository.findByCode(code)
                                                .orElseThrow(() -> new RuntimeException("Role not found: " + code)))
                                .collect(java.util.stream.Collectors.toSet());

                User user = User.builder()
                                .email(req.getEmail())
                                .passwordHash(passwordEncoder.encode(req.getPassword()))
                                .status(UserStatus.ACTIVE)
                                .roles(roles)
                                .createdAt(java.time.LocalDateTime.now())
                                .updatedAt(java.time.LocalDateTime.now())
                                .build();
                userRepository.save(user);

                CustomerProfile profile = CustomerProfile.builder()
                                .user(user)
                                .fullName(req.getFullName())
                                .phone(req.getPhone())
                                .build();
                profileRepository.save(profile);

                return buildDetailDto(user);
        }

        @Override
        @Transactional
        public AdminUserDetailResponse replaceRoles(Long userId, AdminUpdateUserRolesRequest req) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                var roles = req.getRoles().stream()
                                .map(code -> roleRepository.findByCode(code)
                                                .orElseThrow(() -> new RuntimeException("Role not found: " + code)))
                                .collect(java.util.stream.Collectors.toSet());

                user.setRoles(roles);
                user.setUpdatedAt(java.time.LocalDateTime.now());
                userRepository.save(user);
                return buildDetailDto(user);
        }

        @Override
        @Transactional
        public void resetPassword(Long userId, AdminResetPasswordRequest req) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
                user.setUpdatedAt(java.time.LocalDateTime.now());
                userRepository.save(user);
        }

        @Override
        @Transactional
        public void deleteUser(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                user.setStatus(UserStatus.INACTIVE);
                user.setUpdatedAt(java.time.LocalDateTime.now());
                userRepository.save(user);
        }
}
