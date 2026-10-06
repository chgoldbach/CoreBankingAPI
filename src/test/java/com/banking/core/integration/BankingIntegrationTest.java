package com.banking.core.integration;

import com.banking.core.dto.request.CreateAccountRequest;
import com.banking.core.dto.request.LoginRequest;
import com.banking.core.dto.request.RegisterRequest;
import com.banking.core.dto.request.TransferRequest;
import com.banking.core.dto.response.AccountResponse;
import com.banking.core.dto.response.AuthResponse;
import com.banking.core.dto.response.TransactionResponse;
import com.banking.core.entity.Currency;
import com.banking.core.repository.AccountRepository;
import com.banking.core.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Integration Testler.
 *
 * @SpringBootTest: Gerçek uygulama context'ini başlatır.
 * @AutoConfigureMockMvc: HTTP isteklerini gerçek server olmadan simüle eder.
 * H2 in-memory DB kullanılır — gerçek PostgreSQL gerektirmez.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Core Banking API Integration Tests")
class BankingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    private String userToken;
    private UUID userAccountId;
    private String secondUserToken;
    private UUID secondAccountId;

    @BeforeEach
    void setUp() throws Exception {
        // 1. İlk kullanıcı kaydı ve giriş
        AuthResponse auth1 = registerAndLogin("ali@bank.com", "Password1!", "Ali", "Yılmaz");
        userToken = auth1.accessToken();

        // 2. İlk kullanıcı için hesap aç (10000 TRY)
        AccountResponse account1 = createAccount(userToken, Currency.TRY, new BigDecimal("10000.00"));
        userAccountId = account1.id();

        // 3. İkinci kullanıcı kaydı ve giriş
        AuthResponse auth2 = registerAndLogin("ayse@bank.com", "Password2!", "Ayşe", "Kaya");
        secondUserToken = auth2.accessToken();

        // 4. İkinci kullanıcı için hesap aç (5000 TRY)
        AccountResponse account2 = createAccount(secondUserToken, Currency.TRY, new BigDecimal("5000.00"));
        secondAccountId = account2.id();
    }

    // ─── Auth Tests ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Kayıt → Giriş → Token doğrulama — tam akış")
    void authFlow_fullFlow_success() throws Exception {
        // Register
        RegisterRequest registerReq = new RegisterRequest(
                "Mehmet", "Demir", "mehmet@bank.com", "Password3!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("mehmet@bank.com"));

        // Duplicate e-posta → 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Geçersiz kimlik bilgileriyle giriş — 401 döner")
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest badLogin = new LoginRequest("ali@bank.com", "WrongPassword1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT olmadan korumalı endpoint — 403 döner")
    void protectedEndpoint_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isForbidden());
    }

    // ─── Account Tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Hesap oluşturma ve bakiye sorgulama")
    void account_createAndQueryBalance_success() throws Exception {
        // Bakiye sorgulama
        mockMvc.perform(get("/api/accounts/{id}/balance", userAccountId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000.00))
                .andExpect(jsonPath("$.currency").value("TRY"));
    }

    @Test
    @DisplayName("Başkasının bakiyesini sorgulama — 403 döner")
    void account_queryOthersBalance_returns403() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}/balance", secondAccountId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─── Transfer Tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Başarılı para transferi — ACID: bakiyeler doğru güncellenir")
    void transfer_success_balancesUpdated() throws Exception {
        // Transfer: Ali → Ayşe, 3000 TRY
        TransferRequest transferReq = new TransferRequest(
                userAccountId, secondAccountId,
                new BigDecimal("3000.00"), "Test transferi"
        );

        MvcResult result = mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(3000.00))
                .andReturn();

        // Bakiye doğrulama
        mockMvc.perform(get("/api/accounts/{id}/balance", userAccountId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.balance").value(7000.00));

        mockMvc.perform(get("/api/accounts/{id}/balance", secondAccountId)
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(jsonPath("$.balance").value(8000.00));
    }

    @Test
    @DisplayName("Yetersiz bakiye — 422 döner, bakiyeler değişmez")
    void transfer_insufficientFunds_returns422_balancesUnchanged() throws Exception {
        TransferRequest transferReq = new TransferRequest(
                userAccountId, secondAccountId,
                new BigDecimal("99999.00"), "Çok büyük transfer"
        );

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));

        // Bakiyeler değişmemeli
        mockMvc.perform(get("/api/accounts/{id}/balance", userAccountId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.balance").value(10000.00));
    }

    @Test
    @DisplayName("Aynı hesaba transfer — 400 döner")
    void transfer_sameAccount_returns400() throws Exception {
        TransferRequest transferReq = new TransferRequest(
                userAccountId, userAccountId,
                new BigDecimal("100.00"), "Kendime"
        );

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Başkasının hesabından transfer — 403 döner")
    void transfer_fromOthersAccount_returns403() throws Exception {
        // Ayşe, Ali'nin hesabından transfer yapmaya çalışıyor
        TransferRequest transferReq = new TransferRequest(
                userAccountId, secondAccountId,
                new BigDecimal("100.00"), "Yetkisiz"
        );

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("İşlem geçmişi — transfer sonrası listelenir")
    void transactionHistory_afterTransfer_containsRecord() throws Exception {
        // Önce transfer yap
        TransferRequest transferReq = new TransferRequest(
                userAccountId, secondAccountId,
                new BigDecimal("500.00"), "Geçmiş testi"
        );

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isCreated());

        // İşlem geçmişini kontrol et
        mockMvc.perform(get("/api/transactions/history")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("ACID - Eş zamanlı transferler — bakiye tutarlılığı korunur")
    void transfer_concurrent_acidConsistency() throws Exception {
        // 5 eş zamanlı transfer (her biri 1000 TRY)
        int threadCount = 5;
        BigDecimal transferAmount = new BigDecimal("1000.00");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Future<Integer>> futures = new ArrayList<>();

        TransferRequest transferReq = new TransferRequest(
                userAccountId, secondAccountId,
                transferAmount, "Eş zamanlı test"
        );
        String requestBody = objectMapper.writeValueAsString(transferReq);

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await(); // Hepsinin aynı anda başlamasını sağla
                    MvcResult result = mockMvc.perform(post("/api/transactions/transfer")
                                    .header("Authorization", "Bearer " + userToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody))
                            .andReturn();
                    int status = result.getResponse().getStatus();
                    if (status == 201) successCount.incrementAndGet();
                    else failureCount.incrementAndGet();
                    return status;
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    return 500;
                }
            }));
        }

        latch.countDown(); // Tüm thread'leri başlat
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Nihai bakiyeleri kontrol et
        // Başlangıç: Ali=10000, Ayşe=5000
        // Her başarılı transfer: Ali-=1000, Ayşe+=1000
        var aliAccount = accountRepository.findById(userAccountId).orElseThrow();
        var ayseAccount = accountRepository.findById(secondAccountId).orElseThrow();

        BigDecimal expectedAliBalance = new BigDecimal("10000.00")
                .subtract(transferAmount.multiply(BigDecimal.valueOf(successCount.get())));
        BigDecimal expectedAyseBalance = new BigDecimal("5000.00")
                .add(transferAmount.multiply(BigDecimal.valueOf(successCount.get())));

        // Kritik doğrulama: toplam bakiye değişmemeli
        BigDecimal totalBalance = aliAccount.getBalance().add(ayseAccount.getBalance());
        assertThat(totalBalance).isEqualByComparingTo("15000.00");

        // Bireysel bakiyeler beklentiyle eşleşmeli
        assertThat(aliAccount.getBalance()).isEqualByComparingTo(expectedAliBalance);
        assertThat(ayseAccount.getBalance()).isEqualByComparingTo(expectedAyseBalance);
    }

    // ─── Yardımcı metodlar ────────────────────────────────────────────────────

    private AuthResponse registerAndLogin(String email, String password,
                                           String firstName, String lastName) throws Exception {
        RegisterRequest req = new RegisterRequest(firstName, lastName, email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    private AccountResponse createAccount(String token, Currency currency,
                                           BigDecimal initialBalance) throws Exception {
        CreateAccountRequest req = new CreateAccountRequest(currency, initialBalance);
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AccountResponse.class);
    }
}
