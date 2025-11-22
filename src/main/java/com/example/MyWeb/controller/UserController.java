package com.example.MyWeb.controller;

import com.example.MyWeb.dto.user.UserResponse;
import com.example.MyWeb.security.CustomUserDetails;
import com.example.MyWeb.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // GET /api/v1/users/me
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        UserResponse res = userService.getCurrentUser(currentUser.getId());
        return ResponseEntity.ok(res);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal com.example.MyWeb.security.CustomUserDetails currentUser,
            @Valid @RequestBody com.example.MyWeb.dto.user.UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateCurrentUser(currentUser.getId(), req));
    }
}
