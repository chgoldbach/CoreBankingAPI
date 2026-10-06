package com.banking.core.dto.request;

import com.banking.core.entity.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Yeni hesap açma isteği.
 */
public record CreateAccountRequest(

        @NotNull(message = "Para birimi belirtilmeli")
        Currency currency,

        @NotNull(message = "Başlangıç bakiyesi belirtilmeli")
        @DecimalMin(value = "0.00", message = "Başlangıç bakiyesi negatif olamaz")
        BigDecimal initialBalance
) {}
