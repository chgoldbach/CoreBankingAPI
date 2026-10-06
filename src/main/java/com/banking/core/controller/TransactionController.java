package com.banking.core.controller;

import com.banking.core.dto.request.TransferRequest;
import com.banking.core.dto.response.TransactionResponse;
import com.banking.core.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Para transferi ve işlem geçmişi endpoint'leri.
 * Tüm endpoint'ler JWT gerektirir.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }


    /**
     * POST /api/transactions/transfer
     * ACID uyumlu para transferi gerçekleştirir.
     *
     * İşlem başarılıysa 201 Created ve transfer detayı döner.
     * Hata durumunda (yetersiz bakiye, geçersiz hesap vb.) uygun hata kodu döner.
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        TransactionResponse response = transactionService.transfer(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/transactions/history
     * Giriş yapan kullanıcının tüm işlem geçmişini sayfalı döner.
     *
     * @param page Sayfa numarası (0'dan başlar, varsayılan: 0)
     * @param size Sayfa boyutu (varsayılan: 20, max: 100)
     */
    @GetMapping("/history")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Max sayfa boyutunu 100 ile sınırla
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<TransactionResponse> history =
                transactionService.getMyTransactionHistory(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/transactions/{id}
     * Belirli bir transferin detayını döner.
     * Kullanıcı yalnızca kendi işlemlerini görebilir.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        TransactionResponse transaction =
                transactionService.getTransactionById(id, userDetails.getUsername());
        return ResponseEntity.ok(transaction);
    }
}
