package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceAmountValidationTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account mallory;
    private Account bob;

    @BeforeEach
    void setUp() {
        mallory = account(1L, "mallory", "100");
        bob = account(2L, "bob", "1000");
        lenient().when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-500", "0", "-0.01"})
    void transferRejectsNonPositiveAmount(String amount) {
        assertThrows(IllegalArgumentException.class,
                () -> accountService.transferAmount(mallory, "bob", new BigDecimal(amount)));
        assertBalancesUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-500", "0"})
    void withdrawRejectsNonPositiveAmount(String amount) {
        assertThrows(IllegalArgumentException.class,
                () -> accountService.withdraw(mallory, new BigDecimal(amount)));
        assertBalancesUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-500", "0"})
    void depositRejectsNonPositiveAmount(String amount) {
        assertThrows(IllegalArgumentException.class,
                () -> accountService.deposit(mallory, new BigDecimal(amount)));
        assertBalancesUnchanged();
    }

    @Test
    void rejectsNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(mallory, null));
        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(mallory, null));
        assertThrows(IllegalArgumentException.class, () -> accountService.transferAmount(mallory, "bob", null));
        assertBalancesUnchanged();
    }

    @Test
    void transferRejectsSelfTransfer() {
        assertThrows(IllegalArgumentException.class,
                () -> accountService.transferAmount(mallory, "mallory", new BigDecimal("10")));
        assertBalancesUnchanged();
    }

    @Test
    void positiveTransferMovesFunds() {
        accountService.transferAmount(mallory, "bob", new BigDecimal("40"));
        assertEquals(new BigDecimal("60"), mallory.getBalance());
        assertEquals(new BigDecimal("1040"), bob.getBalance());
    }

    private void assertBalancesUnchanged() {
        assertEquals(new BigDecimal("100"), mallory.getBalance());
        assertEquals(new BigDecimal("1000"), bob.getBalance());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    private static Account account(Long id, String username, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setBalance(new BigDecimal(balance));
        return account;
    }
}
