package com.banking.core.dto.response;

import java.math.BigDecimal;

/**
 * Bakiye sorgulama yanıtı — sade ve odaklı.
 */
public record BalanceResponse(
        String accountNumber,
        BigDecimal balance,
        String currency
) {}
