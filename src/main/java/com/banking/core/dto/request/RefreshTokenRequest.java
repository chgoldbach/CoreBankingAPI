package com.banking.core.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh token ile yeni access token alma isteği.
 */
public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token boş olamaz")
        String refreshToken
) {}
