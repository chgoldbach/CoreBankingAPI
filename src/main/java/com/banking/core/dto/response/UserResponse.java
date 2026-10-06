package com.banking.core.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kullanıcı bilgilerini içeren yanıt. Şifre hash'i dahil edilmez.
 */
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        LocalDateTime createdAt
) {}
