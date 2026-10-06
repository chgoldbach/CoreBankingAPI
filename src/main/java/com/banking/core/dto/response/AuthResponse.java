package com.banking.core.dto.response;

/**
 * Kimlik doğrulama yanıtı — access ve refresh token'ları içerir.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,    // saniye cinsinden
        UserResponse user
) {
    public static AuthResponse of(String accessToken, String refreshToken,
                                   long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
