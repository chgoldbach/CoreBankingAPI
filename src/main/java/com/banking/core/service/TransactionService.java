package com.banking.core.service;

import com.banking.core.dto.request.TransferRequest;
import com.banking.core.dto.response.TransactionResponse;
import com.banking.core.entity.Account;
import com.banking.core.entity.Transaction;
import com.banking.core.entity.TransactionStatus;
import com.banking.core.exception.InsufficientFundsException;
import com.banking.core.exception.InvalidTransferException;
import com.banking.core.exception.ResourceNotFoundException;
import com.banking.core.exception.UnauthorizedAccountAccessException;
import com.banking.core.repository.AccountRepository;
import com.banking.core.repository.TransactionRepository;
import com.banking.core.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Para transferi ve işlem geçmişi işlemleri.
 *
 * ═══════════════════════════════════════════════════════════════
 * ACID GARANTİLERİ — transfer() metodu
 * ═══════════════════════════════════════════════════════════════
 *
 * ATOMICITY (Bütünlük):
 *   @Transactional(rollbackFor = Exception.class) sayesinde,
 *   metot içinde herhangi bir exception fırlatılırsa TÜM değişiklikler
 *   geri alınır. Para bir hesaptan çıkıp diğerine girmeden sistem
 *   çökerse: rollback → bakiyeler eski haline döner.
 *
 * CONSISTENCY (Tutarlılık):
 *   - Negatif bakiye kontrolü (@DecimalMin + iş kuralı doğrulaması)
 *   - fromAccount != toAccount kontrolü
 *   - Aynı para birimi kontrolü
 *   Bu kurallar her zaman sağlanır → veritabanı tutarlı kalır.
 *
 * ISOLATION (İzolasyon):
 *   REPEATABLE_READ seviyesi: Transaction süresince okunan veriler
 *   başka transaction tarafından değiştirilemez (phantom read riski azalır).
 *   + PESSIMISTIC_WRITE lock: Aynı hesaba eş zamanlı 2 transfer başlarsa,
 *   ikincisi ilki commit olana kadar bekler → dirty read yok.
 *
 * DURABILITY (Kalıcılık):
 *   Commit sonrası H2/PostgreSQL WAL (Write-Ahead Log) mekanizması
 *   veriyi diske yazar → sistem çökse bile veri kaybolmaz.
 *
 * DEADLOCK ÖNLEMESİ:
 *   Hesaplar her zaman ID'nin lexicografik sırasına göre kilitlenir.
 *   A→B ve B→A aynı anda başlarsa:
 *   Her ikisi de önce küçük ID'li hesabı kilitler → biri bekler → deadlock olmaz.
 * ═══════════════════════════════════════════════════════════════
 */
@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);


    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }


    /**
     * ACID uyumlu para transferi gerçekleştirir.
     *
     * @param request   Transfer detayları (from, to, amount, description)
     * @param userEmail Giriş yapan kullanıcının e-postası (yetki kontrolü için)
     * @return Tamamlanan transfer bilgileri
     */
    @Transactional(
            isolation = Isolation.REPEATABLE_READ,
            rollbackFor = Exception.class
    )
    public TransactionResponse transfer(TransferRequest request, String userEmail) {

        UUID fromId = request.fromAccountId();
        UUID toId = request.toAccountId();

        // ── Kural 1: Aynı hesaba transfer yasak ──────────────────────────────
        if (fromId.equals(toId)) {
            throw new InvalidTransferException("Aynı hesaba transfer yapılamaz");
        }

        // ── Deadlock-safe sıralı kilitleme ────────────────────────────────────
        // ID'leri lexicografik sırayla kilitleyerek deadlock'u önlüyoruz.
        // Thread A: fromId=1, toId=2 → önce 1, sonra 2 kilitler
        // Thread B: fromId=2, toId=1 → önce 1 kilitler, A commit edene kadar bekler
        UUID firstLockId  = fromId.compareTo(toId) < 0 ? fromId : toId;
        UUID secondLockId = fromId.compareTo(toId) < 0 ? toId : fromId;

        Account first = accountRepository.findByIdWithPessimisticLock(firstLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Hesap", "id", firstLockId));
        Account second = accountRepository.findByIdWithPessimisticLock(secondLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Hesap", "id", secondLockId));

        // Hangi hesabın from, hangisinin to olduğunu belirle
        Account fromAccount = firstLockId.equals(fromId) ? first : second;
        Account toAccount   = firstLockId.equals(fromId) ? second : first;

        // ── Kural 2: Sahiplik doğrulaması ─────────────────────────────────────
        // Yalnızca kendi hesabından transfer yapılabilir
        if (!fromAccount.getOwner().getEmail().equals(userEmail)) {
            throw new UnauthorizedAccountAccessException(
                    "Bu hesap size ait değil: " + fromId);
        }

        // ── Kural 3: Hesap aktiflik kontrolü ─────────────────────────────────
        if (!fromAccount.isActive()) {
            throw new InvalidTransferException("Gönderen hesap aktif değil");
        }
        if (!toAccount.isActive()) {
            throw new InvalidTransferException("Alıcı hesap aktif değil");
        }

        // ── Kural 4: Para birimi uyumu ────────────────────────────────────────
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new InvalidTransferException(
                    String.format("Para birimi uyumsuzluğu: %s → %s. Kur dönüşümü desteklenmiyor.",
                            fromAccount.getCurrency(), toAccount.getCurrency()));
        }

        // ── Kural 5: Yeterli bakiye kontrolü ─────────────────────────────────
        if (fromAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(
                    String.format("Yetersiz bakiye. Mevcut: %.2f %s, İstenen: %.2f %s",
                            fromAccount.getBalance(), fromAccount.getCurrency(),
                            request.amount(), fromAccount.getCurrency()));
        }

        // ── Transaction kaydını PENDING olarak oluştur ────────────────────────
        String referenceNumber = UUID.randomUUID().toString();
        Transaction tx = Transaction.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(request.amount())
                .status(TransactionStatus.PENDING)
                .description(request.description())
                .referenceNumber(referenceNumber)
                .build();
        transactionRepository.save(tx);

        // ── Bakiyeleri güncelle ────────────────────────────────────────────────
        // BU SATIRLAR ARASINDA HATA OLURSA → @Transactional rollback devreye girer
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.amount()));
        toAccount.setBalance(toAccount.getBalance().add(request.amount()));

        // ── Transfer tamamlandı ────────────────────────────────────────────────
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setCompletedAt(LocalDateTime.now());

        log.info("Transfer tamamlandı: {} → {} | Tutar: {} {} | Ref: {}",
                fromAccount.getAccountNumber(), toAccount.getAccountNumber(),
                request.amount(), fromAccount.getCurrency(), referenceNumber);

        return toTransactionResponse(tx);
        // Metot başarıyla sonlanırsa → COMMIT
        // Herhangi bir RuntimeException → ROLLBACK (bakiyeler eski haline döner)
    }

    /**
     * Kullanıcıya ait tüm hesaplardaki işlem geçmişini sayfalı döner.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getMyTransactionHistory(String userEmail, Pageable pageable) {
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı"));

        return transactionRepository.findAllByUserId(user.getId(), pageable)
                .map(TransactionService::toTransactionResponse);
    }

    /**
     * Belirli bir transfer işleminin detayını döner.
     * Kullanıcı yalnızca kendi işlemlerine erişebilir.
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID transactionId, String userEmail) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("İşlem", "id", transactionId));

        // Yetki kontrolü: işlemin gönderen veya alan tarafı olmalı
        boolean isInvolved =
                tx.getFromAccount().getOwner().getEmail().equals(userEmail) ||
                tx.getToAccount().getOwner().getEmail().equals(userEmail);

        if (!isInvolved) {
            throw new UnauthorizedAccountAccessException("Bu işleme erişim yetkiniz yok");
        }

        return toTransactionResponse(tx);
    }

    // ─── Yardımcı metodlar ────────────────────────────────────────────────────

    static TransactionResponse toTransactionResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getReferenceNumber(),
                tx.getFromAccount().getId(),
                tx.getFromAccount().getAccountNumber(),
                tx.getToAccount().getId(),
                tx.getToAccount().getAccountNumber(),
                tx.getAmount(),
                tx.getStatus().name(),
                tx.getDescription(),
                tx.getCreatedAt(),
                tx.getCompletedAt()
        );
    }
}
