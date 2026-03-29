package com.siddhesh.QuickCart.Controller;

import com.siddhesh.QuickCart.Dto.ApiResponse;
import com.siddhesh.QuickCart.Dto.AuthResponse;
import com.siddhesh.QuickCart.Dto.LoginRequest;
import com.siddhesh.QuickCart.Dto.RegisterRequest;
import com.siddhesh.QuickCart.Service.Impl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthServiceImpl authService;

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
}
