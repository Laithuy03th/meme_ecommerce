package com.example.MyWeb.controller.admin;

import com.example.MyWeb.dto.auth.LoginRequest;
import com.example.MyWeb.dto.auth.LoginResponse;
import com.example.MyWeb.service.AuthService;
import com.example.MyWeb.util.CookieUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
@Slf4j
public class AdminAuthController {

    private final AuthService authService;

    /**
     * Admin Login
     */
    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        LoginResponse res = authService.login(request);

        // Validate ADMIN role
        boolean isAdmin = res.getUser() != null
                && res.getUser().getRoles() != null
                && (res.getUser().getRoles().contains("ADMIN") || res.getUser().getRoles().contains("ROLE_ADMIN"));

        if (!isAdmin) {
            log.warn("Non-admin login attempt blocked for email: {}", request.getEmail());
            return ResponseEntity.status(403).body(
                    "Tài khoản này không có quyền truy cập Admin Dashboard. " +
                            "Vui lòng đăng nhập bằng tài khoản Admin.");
        }

        // Set ADMIN cookie
        Cookie adminRefreshCookie = CookieUtil.createAdminRefreshTokenCookie(
                res.getRefreshToken(),
                7 * 24 * 60 * 60, // 7 days
                "/");
        response.addCookie(adminRefreshCookie);

        // Remove refresh token
        res.setRefreshToken(null);

        log.info("Admin login successful for: {}", request.getEmail());
        return ResponseEntity.ok(res);
    }

    /**
     * Admin Token Refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> adminRefreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        // Read from ADMIN-specific cookie only
        String adminRefreshToken = CookieUtil.getAdminRefreshTokenFromCookies(request.getCookies());

        if (adminRefreshToken == null || adminRefreshToken.isEmpty()) {
            return ResponseEntity.status(401).body("Missing admin refresh token");
        }

        try {
            LoginResponse loginResponse = authService.refreshToken(adminRefreshToken);

            // Validate ADMIN role
            boolean isAdmin = loginResponse.getUser() != null
                    && loginResponse.getUser().getRoles() != null
                    && (loginResponse.getUser().getRoles().contains("ADMIN")
                            || loginResponse.getUser().getRoles().contains("ROLE_ADMIN"));

            if (!isAdmin) {
                response.addCookie(CookieUtil.deleteAdminRefreshTokenCookie());
                return ResponseEntity.status(403).body("Admin privileges revoked");
            }

            // set new admin cookie
            Cookie newAdminCookie = CookieUtil.createAdminRefreshTokenCookie(
                    loginResponse.getRefreshToken(),
                    7 * 24 * 60 * 60,
                    "/");
            response.addCookie(newAdminCookie);

            loginResponse.setRefreshToken(null);
            return ResponseEntity.ok(loginResponse);

        } catch (Exception e) {
            // Token invalid/expired
            response.addCookie(CookieUtil.deleteAdminRefreshTokenCookie());
            return ResponseEntity.status(401).body("Invalid or expired admin refresh token");
        }
    }

    /**
     * Admin Logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> adminLogout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String accessToken = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        String adminRefreshToken = CookieUtil.getAdminRefreshTokenFromCookies(request.getCookies());

        authService.logout(accessToken, adminRefreshToken);

        response.addCookie(CookieUtil.deleteAdminRefreshTokenCookie());

        log.info("Admin logout completed");
        return ResponseEntity.noContent().build();
    }
}
