package com.example.bankapp;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AccountServicePositiveAmountTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    private Account newAccount(BigDecimal balance) {
        Account account = accountService.registerAccount("t-" + UUID.randomUUID(), "pw");
        account.setBalance(balance);
        return accountRepository.save(account);
    }

    private BigDecimal balanceOf(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getBalance();
    }

    @Test
    void negativeTransferIsRejectedAndLeavesBalancesUnchanged() {
        Account attacker = newAccount(new BigDecimal("100.00"));
        Account victim = newAccount(new BigDecimal("500.00"));

        assertThrows(RuntimeException.class, () -> accountService.transferAmount(
                attacker, victim.getUsername(), new BigDecimal("-1000000.00")));

        assertEquals(0, balanceOf(attacker).compareTo(new BigDecimal("100.00")));
        assertEquals(0, balanceOf(victim).compareTo(new BigDecimal("500.00")));
    }

    @Test
    void zeroAmountIsRejectedForTransferWithdrawAndDeposit() {
        Account account = newAccount(new BigDecimal("100.00"));
        Account other = newAccount(new BigDecimal("100.00"));

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(account, other.getUsername(), BigDecimal.ZERO));
        assertThrows(RuntimeException.class, () -> accountService.withdraw(account, BigDecimal.ZERO));
        assertThrows(RuntimeException.class, () -> accountService.deposit(account, BigDecimal.ZERO));

        assertEquals(0, balanceOf(account).compareTo(new BigDecimal("100.00")));
    }

    @Test
    void negativeWithdrawAndDepositAreRejected() {
        Account account = newAccount(new BigDecimal("100.00"));

        assertThrows(RuntimeException.class, () -> accountService.withdraw(account, new BigDecimal("-50.00")));
        assertThrows(RuntimeException.class, () -> accountService.deposit(account, new BigDecimal("-50.00")));

        assertEquals(0, balanceOf(account).compareTo(new BigDecimal("100.00")));
    }

    @Test
    void positiveTransferStillMovesFunds() {
        Account sender = newAccount(new BigDecimal("100.00"));
        Account recipient = newAccount(new BigDecimal("0.00"));

        accountService.transferAmount(sender, recipient.getUsername(), new BigDecimal("40.00"));

        assertEquals(0, balanceOf(sender).compareTo(new BigDecimal("60.00")));
        assertEquals(0, balanceOf(recipient).compareTo(new BigDecimal("40.00")));
    }
}
