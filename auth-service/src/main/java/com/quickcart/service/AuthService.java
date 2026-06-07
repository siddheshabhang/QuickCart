package com.quickcart.service;


import com.quickcart.dto.AuthResponse;
import com.quickcart.dto.AdminCreateUserRequest;
import com.quickcart.dto.AdminUserResponse;
import com.quickcart.dto.LoginRequest;
import com.quickcart.dto.RefreshTokenRequest;
import com.quickcart.dto.RegisterRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
    AuthResponse loginWithGoogle(OAuth2User oauth2User);
    AdminUserResponse createPrivilegedUser(AdminCreateUserRequest request, String setupKey);
}
