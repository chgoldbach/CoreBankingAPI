package com.banking.core.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Para transferi yanıtı — işlem detaylarını içerir.
 */
public record TransactionResponse(
        UUID id,
        String referenceNumber,
        UUID fromAccountId,
        String fromAccountNumber,
        UUID toAccountId,
        String toAccountNumber,
        BigDecimal amount,
        String status,
        String description,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {}
