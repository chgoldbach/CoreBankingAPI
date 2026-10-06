package com.banking.core.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Kullanıcı giriş isteği.
 */
public record LoginRequest(

        @NotBlank(message = "E-posta boş olamaz")
        @Email(message = "Geçerli bir e-posta adresi girin")
        String email,

        @NotBlank(message = "Şifre boş olamaz")
        String password
) {}
