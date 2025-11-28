package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.user.UpdateProfileRequest;
import com.example.MyWeb.dto.user.UserResponse;
import com.example.MyWeb.model.CustomerProfile;
import com.example.MyWeb.model.Role;
import com.example.MyWeb.model.User;
import com.example.MyWeb.repository.CustomerProfileRepository;
import com.example.MyWeb.repository.UserRepository;
import com.example.MyWeb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final CustomerProfileRepository customerProfileRepository;
        private final com.example.MyWeb.repository.OrderRepository orderRepository;

        @Override
        public UserResponse getCurrentUser(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                CustomerProfile profile = customerProfileRepository.findByUser(user)
                                .orElse(null);

                return toUserResponse(user, profile);
        }

        @Override
        public UserResponse updateCurrentUser(Long userId, UpdateProfileRequest req) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                CustomerProfile profile = customerProfileRepository.findByUser(user)
                                .orElseGet(() -> {
                                        CustomerProfile p = new CustomerProfile();
                                        p.setUser(user);
                                        return p;
                                });

                if (req.getFullName() != null) {
                        profile.setFullName(req.getFullName());
                }
                if (req.getPhone() != null) {
                        profile.setPhone(req.getPhone());
                }
                if (req.getGender() != null) {
                        profile.setGender(req.getGender());
                }
                if (req.getDateOfBirth() != null && !req.getDateOfBirth().isBlank()) {
                        LocalDate dob = LocalDate.parse(req.getDateOfBirth()); // format yyyy-MM-dd
                        profile.setDateOfBirth(dob);
                }

                customerProfileRepository.save(profile);

                return toUserResponse(user, profile);
        }

        private UserResponse toUserResponse(User user, CustomerProfile profile) {
                Set<String> roleCodes = user.getRoles().stream()
                                .map(Role::getCode)
                                .collect(Collectors.toSet());

                Long totalOrders = orderRepository.countByUser_Id(user.getId());
                Double totalSpent = orderRepository.sumTotalSpentByUserId(user.getId());
                if (totalSpent == null)
                        totalSpent = 0.0;

                String membership = "Bronze";
                if (totalSpent > 1000)
                        membership = "Gold";
                else if (totalSpent > 500)
                        membership = "Silver";

                return UserResponse.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .fullName(profile != null ? profile.getFullName() : null)
                                .phone(profile != null ? profile.getPhone() : null)
                                .roles(roleCodes)
                                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                                .memberSince(String.valueOf(user.getCreatedAt().getYear()))
                                .totalOrders(totalOrders.intValue())
                                .totalSpent(totalSpent)
                                .membershipLevel(membership)
                                .verified(true)
                                .build();
        }
}
