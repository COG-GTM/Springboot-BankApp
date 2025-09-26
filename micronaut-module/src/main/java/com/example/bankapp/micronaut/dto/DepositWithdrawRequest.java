package com.example.bankapp.micronaut.dto;

import com.example.bankapp.model.Account;
import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;

@Serdeable
public class DepositWithdrawRequest {
    private Account account;
    private BigDecimal amount;

    public DepositWithdrawRequest() {}

    public DepositWithdrawRequest(Account account, BigDecimal amount) {
        this.account = account;
        this.amount = amount;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
