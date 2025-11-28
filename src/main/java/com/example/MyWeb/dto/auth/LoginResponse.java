package com.example.MyWeb.dto.auth;

import com.example.MyWeb.dto.user.UserResponse;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private UserResponse user;
}
