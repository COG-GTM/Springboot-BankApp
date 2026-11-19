package com.example.bankapp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal amount;
    private String type;
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private TransactionDirection direction;

    private String counterparty;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    public Transaction() {

    }

    public Transaction(BigDecimal amount, String type, LocalDateTime timestamp, Account account) {
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
        this.account = account;
    }

    public Transaction(BigDecimal amount, String type, TransactionDirection direction, String counterparty,
                       LocalDateTime timestamp, Account account) {
        this(amount, type, timestamp, account);
        this.direction = direction;
        this.counterparty = counterparty;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public TransactionDirection getDirection() {
        return direction;
    }

    public void setDirection(TransactionDirection direction) {
        this.direction = direction;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public boolean isCredit() {
        if (direction != null) {
            return direction == TransactionDirection.CREDIT;
        }
        // Rows written before the direction column existed: match only the fixed prefix,
        // never text that may come from a username.
        return "Deposit".equals(type) || (type != null && type.startsWith("Transfer In from "));
    }

    public String getDescription() {
        if (counterparty == null) {
            return type;
        }
        return type + (direction == TransactionDirection.CREDIT ? " from " : " to ") + counterparty;
    }
}
