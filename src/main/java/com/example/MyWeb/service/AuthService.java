package com.example.MyWeb.service;

import com.example.MyWeb.dto.auth.ChangePasswordRequest;
import com.example.MyWeb.dto.auth.ForgotPasswordRequest;
import com.example.MyWeb.dto.auth.LoginRequest;
import com.example.MyWeb.dto.auth.LoginResponse;
import com.example.MyWeb.dto.auth.RegisterRequest;
import com.example.MyWeb.dto.auth.ResetPasswordRequest;
import com.example.MyWeb.dto.user.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void logout(String token); // sẽ blacklist token

}
