package com.example.MyWeb.service;

import com.example.MyWeb.dto.user.UpdateProfileRequest;
import com.example.MyWeb.dto.user.UserResponse;

public interface UserService {

    UserResponse getCurrentUser(Long userId);

    UserResponse updateCurrentUser(Long userId, UpdateProfileRequest req);
}
