package com.example.MyWeb.service.impl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.MyWeb.dto.auth.LoginRequest;
import com.example.MyWeb.dto.auth.LoginResponse;
import com.example.MyWeb.dto.auth.RegisterRequest;
import com.example.MyWeb.dto.user.UserResponse;
import com.example.MyWeb.model.CustomerProfile;
import com.example.MyWeb.model.Role;
import com.example.MyWeb.model.User;
import com.example.MyWeb.repository.CustomerProfileRepository;
import com.example.MyWeb.repository.RoleRepository;
import com.example.MyWeb.repository.UserRepository;
import com.example.MyWeb.security.JwtService;
import com.example.MyWeb.service.AuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final CustomerProfileRepository customerProfileRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtService jwtService;

        @Override
        public UserResponse register(RegisterRequest request) {

                // 1. Check email trùng
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("Email already exists");
                }

                // 2. Lấy role CUSTOMER
                Role customerRole = roleRepository.findByCode("CUSTOMER")
                                .orElseThrow(() -> new RuntimeException("Role CUSTOMER not found"));

                // 3. Tạo User
                User user = User.builder()
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .status("ACTIVE")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .roles(Set.of(customerRole))
                                .build();

                user = userRepository.save(user);

                // 4. Tạo CustomerProfile
                CustomerProfile profile = CustomerProfile.builder()
                                .user(user)
                                .fullName(request.getFullName())
                                .phone(request.getPhone())
                                .build();

                customerProfileRepository.save(profile);

                // 5. Trả về DTO
                return toUserResponse(user, profile);
        }

        private UserResponse toUserResponse(User user, CustomerProfile profile) {
                Set<String> roleCodes = user.getRoles()
                                .stream()
                                .map(Role::getCode)
                                .collect(Collectors.toSet());

                return UserResponse.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .fullName(profile != null ? profile.getFullName() : null)
                                .phone(profile != null ? profile.getPhone() : null)
                                .roles(roleCodes)
                                .build();
        }

        @Override
        public LoginResponse login(LoginRequest request) {

                // 1. Xác thực username/password
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail(),
                                                request.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                // 2. Lấy user + profile từ DB
                User user = userRepository.findByEmail(userDetails.getUsername())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                CustomerProfile profile = customerProfileRepository.findByUser(user)
                                .orElse(null);

                // 3. Tạo JWT
                String token = jwtService.generateToken(
                                user.getEmail(),
                                Map.of("userId", user.getId()));

                UserResponse userResponse = toUserResponse(user, profile);

                return LoginResponse.builder()
                                .accessToken(token)
                                .user(userResponse)
                                .build();
        }
}
