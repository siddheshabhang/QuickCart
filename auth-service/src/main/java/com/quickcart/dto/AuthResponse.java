package com.quickcart.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";

    public AuthResponse(String accessToken) {
        this(accessToken, null);
    }

    public AuthResponse(String accessToken, String refreshToken) {
        this.token = accessToken;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
