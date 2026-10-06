package com.banking.core.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Para transferi isteği.
 *
 * fromAccountId ve toAccountId aynı olamaz — servis katmanında doğrulanır.
 * amount sıfırdan büyük olmalı — validation ile garantilenir.
 */
public record TransferRequest(

        @NotNull(message = "Gönderen hesap ID'si belirtilmeli")
        UUID fromAccountId,

        @NotNull(message = "Alıcı hesap ID'si belirtilmeli")
        UUID toAccountId,

        @NotNull(message = "Transfer tutarı belirtilmeli")
        @DecimalMin(value = "0.01", message = "Transfer tutarı 0'dan büyük olmalı")
        BigDecimal amount,

        @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
        String description
) {}
