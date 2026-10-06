package com.banking.core.controller;

import com.banking.core.dto.request.CreateAccountRequest;
import com.banking.core.dto.response.AccountResponse;
import com.banking.core.dto.response.BalanceResponse;
import com.banking.core.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Bankacılık hesabı endpoint'leri.
 * Tüm endpoint'ler JWT gerektirir.
 *
 * @AuthenticationPrincipal: SecurityContext'ten mevcut kullanıcıyı çeker.
 * Controller'da authentication işlemi yapılmaz — bu JWT Filter'ın sorumluluğudur.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }


    /**
     * POST /api/accounts
     * Giriş yapan kullanıcı adına yeni hesap açar.
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        AccountResponse account = accountService.createAccount(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    /**
     * GET /api/accounts
     * Giriş yapan kullanıcının tüm aktif hesaplarını listeler.
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<AccountResponse> accounts = accountService.getMyAccounts(userDetails.getUsername());
        return ResponseEntity.ok(accounts);
    }

    /**
     * GET /api/accounts/{id}
     * Belirli bir hesabın detaylarını döner.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        AccountResponse account = accountService.getAccountById(id, userDetails.getUsername());
        return ResponseEntity.ok(account);
    }

    /**
     * GET /api/accounts/{id}/balance
     * Hesap bakiyesini döner. Yalnızca hesap sahibi erişebilir.
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        BalanceResponse balance = accountService.getBalance(id, userDetails.getUsername());
        return ResponseEntity.ok(balance);
    }
}
