package com.example.MyWeb.service.impl;

import com.example.MyWeb.repository.PasswordResetTokenRepository;
import com.example.MyWeb.security.JwtBlacklistService;
import com.example.MyWeb.security.JwtService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.MyWeb.dto.auth.ChangePasswordRequest;
import com.example.MyWeb.dto.auth.ForgotPasswordRequest;
import com.example.MyWeb.dto.auth.LoginRequest;
import com.example.MyWeb.dto.auth.LoginResponse;
import com.example.MyWeb.dto.auth.RegisterRequest;
import com.example.MyWeb.dto.auth.ResetPasswordRequest;
import com.example.MyWeb.dto.user.UserResponse;
import com.example.MyWeb.model.CustomerProfile;
import com.example.MyWeb.model.PasswordResetToken;
import com.example.MyWeb.model.Role;
import com.example.MyWeb.model.User;
import com.example.MyWeb.repository.CustomerProfileRepository;
import com.example.MyWeb.repository.RoleRepository;
import com.example.MyWeb.repository.UserRepository;

import com.example.MyWeb.service.AuthService;
import com.example.MyWeb.service.EmailService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.MyWeb.model.enums.UserStatus;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final CustomerProfileRepository customerProfileRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtService jwtService;
        private final PasswordResetTokenRepository passwordResetTokenRepository;
        private final JwtBlacklistService jwtBlacklistService;
        private final EmailService emailService;
        private final com.example.MyWeb.repository.RefreshTokenRepository refreshTokenRepository;

        @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:3000}")
        private String frontendUrl;

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
                                .status(UserStatus.ACTIVE)
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

                // 3. Tạo JWT (Access + Refresh)
                String accessToken = jwtService.generateToken(
                                user.getEmail(),
                                Map.of("userId", user.getId()));

                String refreshToken = jwtService.generateRefreshToken(user.getEmail());

                // L4. Lưu Refresh Token vào Database để kiểm soát vòng đời
                com.example.MyWeb.model.RefreshToken rt = com.example.MyWeb.model.RefreshToken.builder()
                                .user(user)
                                .tokenHash(refreshToken)
                                .expiresAt(java.time.LocalDateTime.now().plusDays(7))
                                .createdAt(java.time.LocalDateTime.now())
                                .revoked(false)
                                .build();
                refreshTokenRepository.save(rt);

                UserResponse userResponse = toUserResponse(user, profile);

                return LoginResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .user(userResponse)
                                .build();
        }

        // Forgot password: sinh token reset, lưu DB, gửi email
        @Override
        @Transactional
        public void forgotPassword(ForgotPasswordRequest req) {
                User user = userRepository.findByEmail(req.getEmail())
                                .orElseThrow(() -> new RuntimeException("Email not found"));

                // Xóa các token cũ của user trước khi tạo token mới (tránh spam tạo quá
                // nhiều token)
                passwordResetTokenRepository.deleteByUser(user);

                String token = java.util.UUID.randomUUID().toString();
                PasswordResetToken prt = PasswordResetToken.builder()
                                .token(token)
                                .user(user)
                                .expiresAt(java.time.LocalDateTime.now().plusMinutes(30))
                                .used(false)
                                .build();
                passwordResetTokenRepository.save(prt);

                String resetLink = frontendUrl + "/reset-password?token=" + token;
                String subject = "Reset Password Request - MemeShop";
                String content = "<h2>Yêu cầu đặt lại mật khẩu</h2>"
                                + "<p>Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản tại MemeShop. Vui lòng click vào nút bên dưới để tiến hành đổi mật khẩu mới:</p>"
                                + "<div style=\"text-align: center; margin: 30px 0;\"><a href=\"" + resetLink
                                + "\" style=\"background-color: #6366f1; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 6px; font-weight: bold;\">Đặt Lại Mật Khẩu</a></div>"
                                + "<p style=\"color: #6b7280; font-size: 14px;\">Đường dẫn này sẽ hết hạn trong vòng 30 phút. Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>";

                emailService.sendHtmlMessage(user.getEmail(), subject, content);
        }

        // Reset password bằng token
        @Override
        public void resetPassword(ResetPasswordRequest req) {
                PasswordResetToken prt = passwordResetTokenRepository.findByToken(req.getToken())
                                .orElseThrow(() -> new RuntimeException("Invalid token"));
                if (prt.isUsed() || prt.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                        throw new RuntimeException("Token expired/used");
                }
                User user = prt.getUser();
                user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
                user.setUpdatedAt(java.time.LocalDateTime.now());
                userRepository.save(user);

                prt.setUsed(true);
                passwordResetTokenRepository.save(prt);
        }

        // Đổi mật khẩu (đang đăng nhập)
        @Override
        public void changePassword(Long userId, ChangePasswordRequest req) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
                        throw new RuntimeException("Old password not match");
                }
                user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
                user.setUpdatedAt(java.time.LocalDateTime.now());
                userRepository.save(user);
        }

        // Logout: đưa token hiện tại vào blacklist và xóa session khỏi DB
        @Override
        @Transactional
        public void logout(String accessToken, String refreshToken) {
                // 1. Blacklist access token nếu còn hiệu lực (bắt exception nếu đã hết hạn thì
                // bỏ qua)
                if (accessToken != null && !accessToken.isEmpty()) {
                        try {
                                long exp = jwtService.getExpirationEpochSeconds(accessToken);
                                jwtBlacklistService.blacklist(accessToken, exp);
                        } catch (Exception e) {
                                // Access token đã hết hạn hoặc không hợp lệ -> Không cần blacklist
                        }
                }
                if (refreshToken != null && !refreshToken.isEmpty()) {
                        refreshTokenRepository.findByTokenHash(refreshToken).ifPresent(rt -> {
                                refreshTokenRepository.delete(rt);
                        });
                }
        }

        // Refresh Token: tạo access token mới từ refresh token
        @Override
        @Transactional
        public LoginResponse refreshToken(String refreshToken) {
                // 1. Validate refresh token
                if (!jwtService.validateToken(refreshToken)) {
                        throw new RuntimeException("Invalid or expired refresh token");
                }

                // L4. Kiểm tra Refresh Token trong DB
                com.example.MyWeb.model.RefreshToken rt = refreshTokenRepository.findByTokenHash(refreshToken)
                                .orElseThrow(() -> new RuntimeException(
                                                "Refresh token not found in DB or has been used/revoked"));

                if (rt.getRevoked() || rt.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                        refreshTokenRepository.delete(rt);
                        throw new RuntimeException("Refresh token revoked or expired");
                }

                // 2. Extract email from refresh token
                String email = jwtService.getSubjectFromToken(refreshToken);

                // 3. Lấy user từ DB
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                if (user.getStatus() != UserStatus.ACTIVE) {
                        refreshTokenRepository.delete(rt);
                        throw new RuntimeException("User account is disabled or locked");
                }

                CustomerProfile profile = customerProfileRepository.findByUser(user)
                                .orElse(null);

                // 4. Tạo access token mới
                String newAccessToken = jwtService.generateToken(
                                user.getEmail(),
                                Map.of("userId", user.getId()));

                // 5. Generate new refresh token (rotating refresh token strategy)
                String newRefreshToken = jwtService.generateRefreshToken(user.getEmail());

                // Xóa Refresh Token cũ, lưu Refresh Token mới (tránh Replay
                // Attack)
                refreshTokenRepository.delete(rt);

                com.example.MyWeb.model.RefreshToken newRt = com.example.MyWeb.model.RefreshToken.builder()
                                .user(user)
                                .tokenHash(newRefreshToken)
                                .expiresAt(java.time.LocalDateTime.now().plusDays(7))
                                .createdAt(java.time.LocalDateTime.now())
                                .revoked(false)
                                .build();
                refreshTokenRepository.save(newRt);

                UserResponse userResponse = toUserResponse(user, profile);

                return LoginResponse.builder()
                                .accessToken(newAccessToken)
                                .refreshToken(newRefreshToken)
                                .user(userResponse)
                                .build();
        }
}
