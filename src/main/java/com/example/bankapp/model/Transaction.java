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

    public Long get_id() {
        return id;
    }

    public void set_id(Long id) {
        this.id = id;
    }

    public BigDecimal get_amount() {
        return amount;
    }

    public void set_amount(BigDecimal amount) {
        this.amount = amount;
    }

    public String get_type() {
        return type;
    }

    public void set_type(String type) {
        this.type = type;
    }

    public LocalDateTime get_timestamp() {
        return timestamp;
    }

    public void set_timestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Account get_account() {
        return account;
    }

    public void set_account(Account account) {
        this.account = account;
    }
}
