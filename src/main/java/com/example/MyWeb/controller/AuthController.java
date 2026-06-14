package com.example.MyWeb.controller;

import com.example.MyWeb.dto.auth.ChangePasswordRequest;
import com.example.MyWeb.dto.auth.ForgotPasswordRequest;
import com.example.MyWeb.dto.auth.LoginRequest;
import com.example.MyWeb.dto.auth.LoginResponse;
import com.example.MyWeb.dto.auth.RegisterRequest;
import com.example.MyWeb.dto.auth.ResetPasswordRequest;
import com.example.MyWeb.dto.user.UserResponse;
import com.example.MyWeb.service.AuthService;
import com.example.MyWeb.util.CookieUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        LoginResponse res = authService.login(request);

        // dùng path "/" cho tất cả mọi người (tránh lỗi kẹt Cookie giữa Admin và
        // Client)
        Cookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(
                res.getRefreshToken(),
                7 * 24 * 60 * 60, // 7 days
                "/");
        response.addCookie(refreshTokenCookie);

        // Remove refresh
        res.setRefreshToken(null);

        return ResponseEntity.ok(res);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        com.example.MyWeb.security.CustomUserDetails userDetails = (com.example.MyWeb.security.CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        authService.changePassword(userId, req);
        return ResponseEntity.noContent().build();
    }

    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        // 1. Trích xuất Access Token (có thể null hoặc hết hạn)
        String accessToken = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        // 2. Trích xuất Refresh Token TỪ COOKIE
        String refreshToken = CookieUtil.getRefreshTokenFromCookies(request.getCookies());

        // 3. Thực thi logic dọn dẹp
        authService.logout(accessToken, refreshToken);

        // 4. Xóa Refresh Token Cookie tại Path chung "/"
        Cookie deleteCookie = CookieUtil.deleteRefreshTokenCookie("/");
        response.addCookie(deleteCookie);

        return ResponseEntity.noContent().build();
    }

    /**
     * Refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        // 1. Get refresh token from HttpOnly cookie
        String refreshToken = CookieUtil.getRefreshTokenFromCookies(request.getCookies());

        // 2. Nếu không có token, trả về 401 (KHÔNG throw RuntimeException)
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(401).body("Missing refresh token");
        }

        try {
            // 3. Rotation: Service sẽ tạo cặp token mới và vô hiệu hóa cái cũ
            LoginResponse loginResponse = authService.refreshToken(refreshToken);

            // 4. Ghi đè Cookie mới (Rotation) với path "/"
            Cookie newRefreshTokenCookie = CookieUtil.createRefreshTokenCookie(
                    loginResponse.getRefreshToken(),
                    7 * 24 * 60 * 60, // 7 days
                    "/");
            response.addCookie(newRefreshTokenCookie);

            // Xóa khỏi body
            loginResponse.setRefreshToken(null);

            return ResponseEntity.ok(loginResponse);

        } catch (Exception e) {
            // Nếu rotation lỗi (do token đã bị dùng hoặc hết hạn), xóa cookie ngay
            response.addCookie(CookieUtil.deleteRefreshTokenCookie("/"));
            return ResponseEntity.status(401).body("Invalid or expired refresh token: " + e.getMessage());
        }
    }
}
