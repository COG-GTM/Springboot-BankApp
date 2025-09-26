package com.example.bankapp.client;

import com.example.bankapp.dto.AccountRequest;
import com.example.bankapp.dto.DepositWithdrawRequest;
import com.example.bankapp.dto.TransactionRequest;
import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.BankingService;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "micronaut-service", url = "${micronaut.service.url:http://micronaut-app:8080}")
public interface MicronautBankingServiceClient extends BankingService {

    @PostMapping("/api/account/find")
    Account findAccountByUsername(@RequestBody AccountRequest request);

    @PostMapping("/api/account/register")
    Account registerAccount(@RequestBody AccountRequest request);

    @PostMapping("/api/account/deposit")
    void deposit(@RequestBody DepositWithdrawRequest request);

    @PostMapping("/api/account/withdraw")
    void withdraw(@RequestBody DepositWithdrawRequest request);

    @PostMapping("/api/account/transactions")
    List<Transaction> getTransactionHistory(@RequestBody TransactionRequest request);

    @PostMapping("/api/account/transfer")
    void transferAmount(@RequestBody TransferRequest request);

    @Override
    default Account findAccountByUsername(String username) {
        return findAccountByUsername(new AccountRequest(username));
    }

    @Override
    default Account registerAccount(String username, String password) {
        return registerAccount(new AccountRequest(username, password));
    }

    @Override
    default void deposit(Account account, BigDecimal amount) {
        deposit(new DepositWithdrawRequest(account, amount));
    }

    @Override
    default void withdraw(Account account, BigDecimal amount) {
        withdraw(new DepositWithdrawRequest(account, amount));
    }

    @Override
    default List<Transaction> getTransactionHistory(Account account) {
        return getTransactionHistory(new TransactionRequest(account));
    }

    @Override
    default void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
        transferAmount(new TransferRequest(fromAccount, toUsername, amount));
    }
}
