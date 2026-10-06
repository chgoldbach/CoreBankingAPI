package com.banking.core.service;

import com.banking.core.dto.request.CreateAccountRequest;
import com.banking.core.dto.response.AccountResponse;
import com.banking.core.dto.response.BalanceResponse;
import com.banking.core.entity.Account;
import com.banking.core.entity.User;
import com.banking.core.exception.ResourceNotFoundException;
import com.banking.core.exception.UnauthorizedAccountAccessException;
import com.banking.core.repository.AccountRepository;
import com.banking.core.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hesap oluşturma, listeleme ve bakiye sorgulama işlemleri.
 */
@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);


    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }


    /**
     * Kimliği doğrulanmış kullanıcı için yeni hesap açar.
     *
     * @param request  Para birimi ve başlangıç bakiyesi
     * @param userEmail Giriş yapan kullanıcının e-postası
     * @return Oluşturulan hesap bilgileri
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, String userEmail) {
        User owner = findUserByEmail(userEmail);

        Account account = Account.builder()
                .accountNumber(generateUniqueAccountNumber())
                .balance(request.initialBalance())
                .currency(request.currency())
                .owner(owner)
                .active(true)
                .build();

        account = accountRepository.save(account);
        log.info("Yeni hesap oluşturuldu: {} | Sahibi: {}", account.getAccountNumber(), userEmail);

        return toAccountResponse(account);
    }

    /**
     * Giriş yapan kullanıcının tüm aktif hesaplarını listeler.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(String userEmail) {
        User owner = findUserByEmail(userEmail);
        return accountRepository.findActiveByOwnerId(owner.getId())
                .stream()
                .map(AccountService::toAccountResponse)
                .collect(Collectors.toList());
    }

    /**
     * Belirtilen hesabın bakiyesini döner.
     * Kullanıcı yalnızca kendi hesabını sorgulayabilir.
     *
     * @param accountId Sorgulanacak hesap ID'si
     * @param userEmail Giriş yapan kullanıcının e-postası
     * @return Bakiye bilgisi
     * @throws UnauthorizedAccountAccessException Başkasının hesabını sorgularsa
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID accountId, String userEmail) {
        Account account = findAccountById(accountId);
        validateOwnership(account, userEmail);

        return new BalanceResponse(
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency().name()
        );
    }

    /**
     * Hesap ID'sinden hesap detayını döner.
     * Sahiplik doğrulaması yapılır.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID accountId, String userEmail) {
        Account account = findAccountById(accountId);
        validateOwnership(account, userEmail);
        return toAccountResponse(account);
    }

    // ─── Dahili yardımcı metodlar ─────────────────────────────────────────────

    Account findAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hesap", "id", accountId));
    }

    void validateOwnership(Account account, String userEmail) {
        if (!account.getOwner().getEmail().equals(userEmail)) {
            throw new UnauthorizedAccountAccessException(
                    "Bu hesap size ait değil: " + account.getId());
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    /**
     * TR ile başlayan benzersiz IBAN-benzeri hesap numarası üretir.
     * Çakışma olursa yeniden dener.
     */
    private String generateUniqueAccountNumber() {
        String accountNumber;
        int attempts = 0;
        do {
            accountNumber = generateAccountNumber();
            attempts++;
            if (attempts > 10) {
                throw new IllegalStateException("Benzersiz hesap numarası üretilemedi");
            }
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder("TR");
        for (int i = 0; i < 24; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    static AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency().name(),
                account.isActive(),
                account.getCreatedAt()
        );
    }
}
