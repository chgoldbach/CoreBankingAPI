package com.banking.core.service;

import com.banking.core.dto.request.TransferRequest;
import com.banking.core.dto.response.TransactionResponse;
import com.banking.core.entity.*;
import com.banking.core.exception.InsufficientFundsException;
import com.banking.core.exception.InvalidTransferException;
import com.banking.core.exception.UnauthorizedAccountAccessException;
import com.banking.core.repository.AccountRepository;
import com.banking.core.repository.TransactionRepository;
import com.banking.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * TransactionService için unit testler.
 * Veritabanı bağlantısı gerektirmez — repository'ler mock'lanır.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    // Test verileri
    private User owner;
    private User anotherUser;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(UUID.randomUUID())
                .email("ali@bank.com")
                .firstName("Ali")
                .lastName("Yılmaz")
                .role(Role.USER)
                .active(true)
                .build();

        anotherUser = User.builder()
                .id(UUID.randomUUID())
                .email("ayse@bank.com")
                .firstName("Ayşe")
                .lastName("Kaya")
                .role(Role.USER)
                .active(true)
                .build();

        fromAccount = Account.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .accountNumber("TR100000000000000000000001")
                .balance(new BigDecimal("10000.00"))
                .currency(Currency.TRY)
                .owner(owner)
                .active(true)
                .version(0L)
                .build();

        toAccount = Account.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .accountNumber("TR100000000000000000000002")
                .balance(new BigDecimal("5000.00"))
                .currency(Currency.TRY)
                .owner(anotherUser)
                .active(true)
                .version(0L)
                .build();

        // Mock: Pessimistic lock ile hesap getirme
        when(accountRepository.findByIdWithPessimisticLock(fromAccount.getId()))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithPessimisticLock(toAccount.getId()))
                .thenReturn(Optional.of(toAccount));

        // Mock: Transaction kaydetme — gelen nesneyi geri döndür
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("Başarılı Transfer Senaryoları")
    class SuccessfulTransfers {

        @Test
        @DisplayName("Geçerli transfer — bakiyeler doğru güncellenir")
        void transfer_validRequest_updatesBalancesCorrectly() {
            // Arrange
            TransferRequest request = new TransferRequest(
                    fromAccount.getId(), toAccount.getId(),
                    new BigDecimal("1000.00"), "Test transferi");

            // Act
            TransactionResponse response = transactionService.transfer(request, owner.getEmail());

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo("COMPLETED");
            assertThat(response.amount()).isEqualByComparingTo("1000.00");

            // Bakiye değişimleri doğrula
            assertThat(fromAccount.getBalance()).isEqualByComparingTo("9000.00");
            assertThat(toAccount.getBalance()).isEqualByComparingTo("6000.00");

            // Transaction kaydının oluşturulduğunu doğrula
            verify(transactionRepository, times(1)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Tam bakiye transferi — hesap sıfırlanır ama işlem tamamlanır")
        void transfer_exactBalance_succeeds() {
            // Arrange
            TransferRequest request = new TransferRequest(
                    fromAccount.getId(), toAccount.getId(),
                    new BigDecimal("10000.00"), null);

            // Act
            TransactionResponse response = transactionService.transfer(request, owner.getEmail());

            // Assert
            assertThat(response.status()).isEqualTo("COMPLETED");
            assertThat(fromAccount.getBalance()).isEqualByComparingTo("0.00");
            assertThat(toAccount.getBalance()).isEqualByComparingTo("15000.00");
        }
    }

    @Nested
    @DisplayName("Başarısız Transfer Senaryoları")
    class FailedTransfers {

        @Test
        @DisplayName("Yetersiz bakiye — InsufficientFundsException fırlatılır")
        void transfer_insufficientBalance_throwsException() {
            // Arrange
            TransferRequest request = new TransferRequest(
                    fromAccount.getId(), toAccount.getId(),
                    new BigDecimal("99999.00"), "Çok büyük tutar");

            // Act & Assert
            assertThatThrownBy(() ->
                    transactionService.transfer(request, owner.getEmail()))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Yetersiz bakiye");

            // Bakiyeler değişmemeli
            assertThat(fromAccount.getBalance()).isEqualByComparingTo("10000.00");
            assertThat(toAccount.getBalance()).isEqualByComparingTo("5000.00");
        }

        @Test
        @DisplayName("Aynı hesaba transfer — InvalidTransferException fırlatılır")
        void transfer_sameAccount_throwsException() {
            // Arrange
            TransferRequest request = new TransferRequest(
                    fromAccount.getId(), fromAccount.getId(),
                    new BigDecimal("100.00"), "Kendime transfer");

            // Act & Assert
            assertThatThrownBy(() ->
                    transactionService.transfer(request, owner.getEmail()))
                    .isInstanceOf(InvalidTransferException.class)
                    .hasMessageContaining("Aynı hesaba");

            // Repository hiç çağrılmamalı
            verifyNoInteractions(accountRepository);
        }

        @Test
        @DisplayName("Başkasının hesabından transfer — UnauthorizedAccountAccessException fırlatılır")
        void transfer_notOwner_throwsException() {
            // Arrange: "ayse@bank.com" fromAccount'a (ali'nin hesabı) transfer yapmaya çalışıyor
            TransferRequest request = new TransferRequest(
                    fromAccount.getId(), toAccount.getId(),
                    new BigDecimal("100.00"), "Yetkisiz transfer");

            // Act & Assert
            assertThatThrownBy(() ->
                    transactionService.transfer(request, anotherUser.getEmail()))
                    .isInstanceOf(UnauthorizedAccountAccessException.class)
                    .hasMessageContaining("Bu hesap size ait değil");
        }

        @Test
        @DisplayName("Farklı para birimi — InvalidTransferException fırlatılır")
        void transfer_currencyMismatch_throwsException() {
            // Arrange
            toAccount.setCurrency(Currency.USD);
            TransferRequest request = new TransferRequest(
                    fromAccount.getId(), toAccount.getId(),
                    new BigDecimal("100.00"), "Kur dönüşümü denemesi");

            // Act & Assert
            assertThatThrownBy(() ->
                    transactionService.transfer(request, owner.getEmail()))
                    .isInstanceOf(InvalidTransferException.class)
                    .hasMessageContaining("Para birimi uyumsuzluğu");
        }

        @Test
        @DisplayName("Pasif hesaptan transfer — InvalidTransferException fırlatılır")
        void transfer_inactiveFromAccount_throwsException() {
            // Arrange
            fromAccount.setActive(false);
            TransferRequest request = new TransferRequest(
                    fromAccount.getId(), toAccount.getId(),
                    new BigDecimal("100.00"), "Pasif hesap denemesi");

            // Act & Assert
            assertThatThrownBy(() ->
                    transactionService.transfer(request, owner.getEmail()))
                    .isInstanceOf(InvalidTransferException.class)
                    .hasMessageContaining("aktif değil");
        }
    }
}
