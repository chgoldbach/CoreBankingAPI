package com.banking.core.controller;

import com.banking.core.dto.request.LoginRequest;
import com.banking.core.dto.request.RefreshTokenRequest;
import com.banking.core.dto.request.RegisterRequest;
import com.banking.core.dto.response.AuthResponse;
import com.banking.core.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Kimlik doğrulama endpoint'leri — JWT token döndürür.
 * Bu endpoint'ler public'tir; SecurityConfig'de permitAll ile işaretlenmiştir.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    /**
     * POST /api/auth/register
     * Yeni kullanıcı kaydı yapar. Başarıyla oluşturulunca 201 + token döner.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Kullanıcı girişi — doğru kimlik bilgileriyle access+refresh token döner.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/refresh
     * Geçerli refresh token ile yeni access token alır.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}
