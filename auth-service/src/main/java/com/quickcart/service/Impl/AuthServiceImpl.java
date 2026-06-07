package com.quickcart.service.Impl;

import com.quickcart.common.exception.DuplicateResourceException;
import com.quickcart.dto.AdminCreateUserRequest;
import com.quickcart.dto.AdminUserResponse;
import com.quickcart.dto.AuthResponse;
import com.quickcart.dto.LoginRequest;
import com.quickcart.dto.RefreshTokenRequest;
import com.quickcart.dto.RegisterRequest;
import com.quickcart.entity.AuthProvider;
import com.quickcart.entity.Role;
import com.quickcart.entity.User;
import com.quickcart.repository.UserRepository;
import com.quickcart.security.JwtService;
import com.quickcart.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Value("${quickcart.admin.setup-key:}")
    private String adminSetupKey;

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

        User savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid Credentials"));

        if(user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid Credentials");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        if (refreshToken == null || !jwtService.isRefreshTokenValid(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        return buildAuthResponse(user);
    }

    public AuthResponse loginWithGoogle(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Google account did not provide an email");
        }

        Object verified = oauth2User.getAttribute("email_verified");
        if (verified != null && !Boolean.parseBoolean(String.valueOf(verified))) {
            throw new BadCredentialsException("Google email is not verified");
        }

        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> userRepository.save(User.builder()
                        .name(resolveGoogleName(oauth2User, normalizedEmail))
                        .email(normalizedEmail)
                        .role(Role.CUSTOMER)
                        .provider(AuthProvider.GOOGLE)
                        .build()));

        if (user.getRole() != Role.CUSTOMER) {
            throw new BadCredentialsException("Google login is only available for customer accounts");
        }

        return buildAuthResponse(user);
    }

    public AdminUserResponse createPrivilegedUser(AdminCreateUserRequest request, String setupKey) {
        validateAdminSetupKey(setupKey);

        Role role = parsePrivilegedRole(request.getRole());
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if(userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }

        User savedUser = userRepository.save(User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .provider(AuthProvider.LOCAL)
                .build());

        return new AdminUserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name());
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name(), user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), user.getRole().name(), user.getId());
        return new AuthResponse(accessToken, refreshToken);
    }

    private String resolveGoogleName(OAuth2User oauth2User, String email) {
        String name = oauth2User.getAttribute("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        return email.substring(0, email.indexOf("@"));
    }

    private void validateAdminSetupKey(String setupKey) {
        if (!StringUtils.hasText(adminSetupKey)) {
            throw new AccessDeniedException("Admin setup key is not configured");
        }
        if (!adminSetupKey.equals(setupKey)) {
            throw new AccessDeniedException("Invalid admin setup key");
        }
    }

    private Role parsePrivilegedRole(String roleValue) {
        Role role;
        try {
            role = Role.valueOf(roleValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Role must be STORE or DELIVERY");
        }

        if (role != Role.STORE && role != Role.DELIVERY) {
            throw new IllegalArgumentException("Role must be STORE or DELIVERY");
        }

        return role;
    }
}
