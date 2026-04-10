package com.siddhesh.QuickCart.Service;

import com.siddhesh.QuickCart.Dto.AuthResponse;
import com.siddhesh.QuickCart.Dto.LoginRequest;
import com.siddhesh.QuickCart.Dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
