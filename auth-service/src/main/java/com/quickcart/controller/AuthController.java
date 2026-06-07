package com.quickcart.controller;

import com.quickcart.common.dto.ApiResponse;
import com.quickcart.dto.AdminCreateUserRequest;
import com.quickcart.dto.AdminUserResponse;
import com.quickcart.dto.AuthResponse;
import com.quickcart.dto.LoginRequest;
import com.quickcart.dto.RefreshTokenRequest;
import com.quickcart.dto.RegisterRequest;
import com.quickcart.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "User registered successfully!",
                        authService.register(request))
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "User logged in successfully",
                        authService.login(request))
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Token refreshed successfully",
                        authService.refresh(request))
        );
    }

    @PostMapping("/admin/users")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createPrivilegedUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @RequestHeader(value = "X-Admin-Setup-Key", required = false) String setupKey) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Privileged user created successfully",
                        authService.createPrivilegedUser(request, setupKey))
        );
    }
}
