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

    /**
     * Login endpoint
     * Returns access token in response body
     * Stores refresh token in HttpOnly cookie for security (XSS protection)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        LoginResponse res = authService.login(request);

        // Store refresh token in HttpOnly cookie (secure against XSS)
        Cookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(res.getRefreshToken());
        response.addCookie(refreshTokenCookie);

        // Remove refresh token from response body for security
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
     * Blacklists access token and deletes refresh token cookie
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        // Blacklist access token
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            authService.logout(token);
        }

        // Delete refresh token cookie
        Cookie deleteCookie = CookieUtil.deleteRefreshTokenCookie();
        response.addCookie(deleteCookie);

        return ResponseEntity.noContent().build();
    }

    /**
     * Refresh token endpoint
     * Gets refresh token from HttpOnly cookie (not request body)
     * Returns new access token and sets new refresh token in cookie
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        // Get refresh token from HttpOnly cookie
        String refreshToken = CookieUtil.getRefreshTokenFromCookies(request.getCookies());

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new RuntimeException("Refresh token not found in cookies. Please login again.");
        }

        // Generate new tokens
        LoginResponse loginResponse = authService.refreshToken(refreshToken);

        // Set new refresh token in HttpOnly cookie (token rotation)
        Cookie newRefreshTokenCookie = CookieUtil.createRefreshTokenCookie(loginResponse.getRefreshToken());
        response.addCookie(newRefreshTokenCookie);

        // Remove refresh token from response body
        loginResponse.setRefreshToken(null);

        return ResponseEntity.ok(loginResponse);
    }
}
