package com.banking.core.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "accounts",
        indexes = {
                @Index(name = "idx_account_number", columnList = "account_number", unique = true),
                @Index(name = "idx_account_owner", columnList = "owner_id")
        })
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_number", unique = true, nullable = false, length = 26)
    private String accountNumber;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency = Currency.TRY;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "fromAccount", fetch = FetchType.LAZY)
    private List<Transaction> outgoingTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "toAccount", fetch = FetchType.LAZY)
    private List<Transaction> incomingTransactions = new ArrayList<>();

    public Account() {}

    public Account(UUID id, String accountNumber, BigDecimal balance, Currency currency, boolean active, Long version, User owner, LocalDateTime createdAt, LocalDateTime updatedAt, List<Transaction> outgoingTransactions, List<Transaction> incomingTransactions) {
        this.id = id;
        this.accountNumber = accountNumber;
        if (balance != null) this.balance = balance;
        if (currency != null) this.currency = currency;
        this.active = active;
        this.version = version;
        this.owner = owner;
        if (createdAt != null) this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        if (outgoingTransactions != null) this.outgoingTransactions = outgoingTransactions;
        if (incomingTransactions != null) this.incomingTransactions = incomingTransactions;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<Transaction> getOutgoingTransactions() { return outgoingTransactions; }
    public void setOutgoingTransactions(List<Transaction> outgoingTransactions) { this.outgoingTransactions = outgoingTransactions; }
    public List<Transaction> getIncomingTransactions() { return incomingTransactions; }
    public void setIncomingTransactions(List<Transaction> incomingTransactions) { this.incomingTransactions = incomingTransactions; }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public static AccountBuilder builder() { return new AccountBuilder(); }

    public static class AccountBuilder {
        private UUID id;
        private String accountNumber;
        private BigDecimal balance = BigDecimal.ZERO;
        private Currency currency = Currency.TRY;
        private boolean active = true;
        private Long version;
        private User owner;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt;
        private List<Transaction> outgoingTransactions = new ArrayList<>();
        private List<Transaction> incomingTransactions = new ArrayList<>();

        public AccountBuilder id(UUID id) { this.id = id; return this; }
        public AccountBuilder accountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
        public AccountBuilder balance(BigDecimal balance) { this.balance = balance; return this; }
        public AccountBuilder currency(Currency currency) { this.currency = currency; return this; }
        public AccountBuilder active(boolean active) { this.active = active; return this; }
        public AccountBuilder version(Long version) { this.version = version; return this; }
        public AccountBuilder owner(User owner) { this.owner = owner; return this; }
        public AccountBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public AccountBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public AccountBuilder outgoingTransactions(List<Transaction> outgoingTransactions) { this.outgoingTransactions = outgoingTransactions; return this; }
        public AccountBuilder incomingTransactions(List<Transaction> incomingTransactions) { this.incomingTransactions = incomingTransactions; return this; }
        public Account build() { return new Account(id, accountNumber, balance, currency, active, version, owner, createdAt, updatedAt, outgoingTransactions, incomingTransactions); }
    }
}
