package com.example.bankapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stored_value_transaction", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stored_value_txn_idempotency", columnNames = {"card_id", "idempotency_key"})
})
public class StoredValueTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private StoredValueCard card;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private StoredValueTransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public StoredValueTransaction() {
    }

    public StoredValueTransaction(StoredValueCard card, StoredValueTransactionType type, BigDecimal amount,
                                  BigDecimal balanceAfter, String idempotencyKey, Instant createdAt) {
        this.card = card;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StoredValueCard getCard() {
        return card;
    }

    public void setCard(StoredValueCard card) {
        this.card = card;
    }

    public StoredValueTransactionType getType() {
        return type;
    }

    public void setType(StoredValueTransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
