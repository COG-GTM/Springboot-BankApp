package com.example.bankapp.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private BigDecimal balance;

    @OneToMany(mappedBy = "account")
    private List<Transaction> transactions;

    @Transient
    private Collection<? extends GrantedAuthority> authorities;

    public Account() {

    }

    public Account(String username, String password, BigDecimal balance, List<Transaction> transactions, Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.balance = balance;
        this.transactions = transactions;
        this.authorities = authorities;
    }

    public Collection<? extends GrantedAuthority> get_authorities() {
        return authorities;
    }

    public void set_authorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    public Long get_id() {
        return id;
    }

    public void set_id(Long id) {
        this.id = id;
    }

    public String get_username() {
        return username;
    }

    public void set_username(String username) {
        this.username = username;
    }

    public String get_password() {
        return password;
    }

    public void set_password(String password) {
        this.password = password;
    }

    public BigDecimal get_balance() {
        return balance;
    }

    public void set_balance(BigDecimal balance) {
        this.balance = balance;
    }

    public List<Transaction> get_transactions() {
        return transactions;
    }

    public void set_transactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
}
