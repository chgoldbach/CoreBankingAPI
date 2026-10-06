package com.banking.core.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Yeni kullanıcı kayıt isteği.
 */
public record RegisterRequest(

        @NotBlank(message = "Ad boş olamaz")
        @Size(min = 2, max = 100, message = "Ad 2-100 karakter arasında olmalı")
        String firstName,

        @NotBlank(message = "Soyad boş olamaz")
        @Size(min = 2, max = 100, message = "Soyad 2-100 karakter arasında olmalı")
        String lastName,

        @NotBlank(message = "E-posta boş olamaz")
        @Email(message = "Geçerli bir e-posta adresi girin")
        String email,

        @NotBlank(message = "Şifre boş olamaz")
        @Size(min = 8, message = "Şifre en az 8 karakter olmalı")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Şifre en az bir büyük harf, bir küçük harf ve bir rakam içermelidir"
        )
        String password
) {}
