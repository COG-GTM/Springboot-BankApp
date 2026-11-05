package com.example.bankapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stored_value_card")
public class StoredValueCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_token", nullable = false, unique = true, length = 64)
    private String cardToken;

    /**
     * PAN-equivalent card reference. Never exposed by the API and never logged.
     */
    @Column(name = "card_reference", nullable = false, length = 32)
    private String cardReference;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "initial_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialAmount;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private StoredValueCardStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    public StoredValueCard() {
    }

    public StoredValueCard(String cardToken, String cardReference, String currency, BigDecimal initialAmount,
                           Instant expiresAt, Instant issuedAt) {
        this.cardToken = cardToken;
        this.cardReference = cardReference;
        this.currency = currency;
        this.initialAmount = initialAmount;
        this.balance = initialAmount;
        this.status = StoredValueCardStatus.ACTIVE;
        this.expiresAt = expiresAt;
        this.issuedAt = issuedAt;
    }

    public boolean isExpiredAt(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    /**
     * Moves an active card to EXPIRED once its expiry has passed.
     */
    public void applyExpiry(Instant now) {
        if (status == StoredValueCardStatus.ACTIVE && isExpiredAt(now)) {
            status = StoredValueCardStatus.EXPIRED;
        }
    }

    /**
     * Masked token for log lines; the full token and the card reference are never logged.
     */
    public String maskedToken() {
        return maskToken(cardToken);
    }

    public static String maskToken(String cardToken) {
        if (cardToken == null || cardToken.length() <= 4) {
            return "****";
        }
        return "****" + cardToken.substring(cardToken.length() - 4);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardToken() {
        return cardToken;
    }

    public void setCardToken(String cardToken) {
        this.cardToken = cardToken;
    }

    public String getCardReference() {
        return cardReference;
    }

    public void setCardReference(String cardReference) {
        this.cardReference = cardReference;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getInitialAmount() {
        return initialAmount;
    }

    public void setInitialAmount(BigDecimal initialAmount) {
        this.initialAmount = initialAmount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public StoredValueCardStatus getStatus() {
        return status;
    }

    public void setStatus(StoredValueCardStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }
}
