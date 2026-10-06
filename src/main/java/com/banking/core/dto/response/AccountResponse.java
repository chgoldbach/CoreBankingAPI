package com.banking.core.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Hesap bilgilerini içeren yanıt.
 */
public record AccountResponse(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        String currency,
        boolean active,
        LocalDateTime createdAt
) {}
