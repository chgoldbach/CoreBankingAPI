package com.banking.core.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions",
        indexes = {
                @Index(name = "idx_tx_from_account", columnList = "from_account_id"),
                @Index(name = "idx_tx_to_account", columnList = "to_account_id"),
                @Index(name = "idx_tx_created_at", columnList = "created_at")
        })
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reference_number", unique = true, length = 36)
    private String referenceNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Transaction() {}

    public Transaction(UUID id, Account fromAccount, Account toAccount, BigDecimal amount, TransactionStatus status, String description, String referenceNumber, LocalDateTime createdAt, LocalDateTime completedAt) {
        this.id = id;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        if (status != null) this.status = status;
        this.description = description;
        this.referenceNumber = referenceNumber;
        if (createdAt != null) this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Account getFromAccount() { return fromAccount; }
    public void setFromAccount(Account fromAccount) { this.fromAccount = fromAccount; }
    public Account getToAccount() { return toAccount; }
    public void setToAccount(Account toAccount) { this.toAccount = toAccount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public static TransactionBuilder builder() { return new TransactionBuilder(); }

    public static class TransactionBuilder {
        private UUID id;
        private Account fromAccount;
        private Account toAccount;
        private BigDecimal amount;
        private TransactionStatus status = TransactionStatus.PENDING;
        private String description;
        private String referenceNumber;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime completedAt;

        public TransactionBuilder id(UUID id) { this.id = id; return this; }
        public TransactionBuilder fromAccount(Account fromAccount) { this.fromAccount = fromAccount; return this; }
        public TransactionBuilder toAccount(Account toAccount) { this.toAccount = toAccount; return this; }
        public TransactionBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public TransactionBuilder status(TransactionStatus status) { this.status = status; return this; }
        public TransactionBuilder description(String description) { this.description = description; return this; }
        public TransactionBuilder referenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; return this; }
        public TransactionBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public TransactionBuilder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Transaction build() { return new Transaction(id, fromAccount, toAccount, amount, status, description, referenceNumber, createdAt, completedAt); }
    }
}
