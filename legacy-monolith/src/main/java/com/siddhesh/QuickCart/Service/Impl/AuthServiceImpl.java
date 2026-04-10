package com.siddhesh.QuickCart.Service.Impl;

import com.siddhesh.QuickCart.Dto.AuthResponse;
import com.siddhesh.QuickCart.Dto.LoginRequest;
import com.siddhesh.QuickCart.Dto.RegisterRequest;
import com.siddhesh.QuickCart.Entity.AuthProvider;
import com.siddhesh.QuickCart.Entity.Role;
import com.siddhesh.QuickCart.Entity.User;
import com.siddhesh.QuickCart.Exception.DuplicateResourceException;
import com.siddhesh.QuickCart.Repository.UserRepository;
import com.siddhesh.QuickCart.Security.JwtService;
import com.siddhesh.QuickCart.Service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthResponse register(RegisterRequest request) {
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .provider(AuthProvider.LOCAL)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid Credentials"));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid Credentials");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token);
    }
}
