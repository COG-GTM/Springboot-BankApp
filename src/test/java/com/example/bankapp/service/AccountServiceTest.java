package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    private Account alice;
    private Account bob;

    @BeforeEach
    void setUp() {
        alice = account(1L, "alice", "100.00");
        bob = account(2L, "bob", "50.00");
    }

    private static Account account(Long id, String username, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setPassword("encoded");
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    private Transaction captureSingleTransaction() {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        return captor.getValue();
    }

    private List<Transaction> captureTransactions(int expected) {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(expected)).save(captor.capture());
        return captor.getAllValues();
    }

    // ---- deposit ----

    @Test
    void deposit_increasesBalanceAndPersistsDepositTransaction() {
        accountService.deposit(alice, new BigDecimal("25.50"));

        assertEquals(new BigDecimal("125.50"), alice.getBalance());
        verify(accountRepository).save(alice);

        Transaction tx = captureSingleTransaction();
        assertEquals(new BigDecimal("25.50"), tx.getAmount());
        assertEquals("Deposit", tx.getType());
        assertSame(alice, tx.getAccount());
    }

    @Test
    void deposit_negativeAmount_isCurrentlyAccepted() {
        // LIKELY BUG: deposit performs no amount validation, so a negative
        // "deposit" silently reduces the balance and is recorded as a Deposit.
        accountService.deposit(alice, new BigDecimal("-40.00"));

        assertEquals(new BigDecimal("60.00"), alice.getBalance());
        Transaction tx = captureSingleTransaction();
        assertEquals(new BigDecimal("-40.00"), tx.getAmount());
        assertEquals("Deposit", tx.getType());
    }

    // ---- withdraw ----

    @Test
    void withdraw_decrementsBalanceAndPersistsWithdrawalTransaction() {
        accountService.withdraw(alice, new BigDecimal("30.00"));

        assertEquals(new BigDecimal("70.00"), alice.getBalance());
        verify(accountRepository).save(alice);

        Transaction tx = captureSingleTransaction();
        assertEquals(new BigDecimal("30.00"), tx.getAmount());
        assertEquals("Withdrawal", tx.getType());
        assertSame(alice, tx.getAccount());
    }

    @Test
    void withdraw_amountGreaterThanBalance_throwsAndSavesNothing() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(alice, new BigDecimal("100.01")));

        assertEquals("Insufficient funds", ex.getMessage());
        assertEquals(new BigDecimal("100.00"), alice.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void withdraw_amountEqualToBalance_succeedsAndLeavesZero() {
        accountService.withdraw(alice, new BigDecimal("100.00"));

        assertEquals(0, alice.getBalance().compareTo(BigDecimal.ZERO));
        verify(accountRepository).save(alice);
        Transaction tx = captureSingleTransaction();
        assertEquals(new BigDecimal("100.00"), tx.getAmount());
        assertEquals("Withdrawal", tx.getType());
    }

    @Test
    void withdraw_negativeAmount_isCurrentlyAccepted() {
        // LIKELY BUG: a negative withdrawal passes the balance check and
        // increases the balance, recorded as a Withdrawal.
        accountService.withdraw(alice, new BigDecimal("-40.00"));

        assertEquals(new BigDecimal("140.00"), alice.getBalance());
        Transaction tx = captureSingleTransaction();
        assertEquals(new BigDecimal("-40.00"), tx.getAmount());
        assertEquals("Withdrawal", tx.getType());
    }

    // ---- transferAmount ----

    @Test
    void transferAmount_debitsSenderCreditsRecipientAndSavesBothTransactions() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        accountService.transferAmount(alice, "bob", new BigDecimal("40.00"));

        assertEquals(new BigDecimal("60.00"), alice.getBalance());
        assertEquals(new BigDecimal("90.00"), bob.getBalance());
        verify(accountRepository).save(alice);
        verify(accountRepository).save(bob);

        List<Transaction> txs = captureTransactions(2);
        Transaction out = txs.get(0);
        Transaction in = txs.get(1);

        assertEquals("Transfer Out to bob", out.getType());
        assertEquals(new BigDecimal("40.00"), out.getAmount());
        assertSame(alice, out.getAccount());

        assertEquals("Transfer In from alice", in.getType());
        assertEquals(new BigDecimal("40.00"), in.getAmount());
        assertSame(bob, in.getAccount());
    }

    @Test
    void transferAmount_insufficientFunds_throwsBeforeRecipientLookupAndSavesNothing() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(alice, "bob", new BigDecimal("100.01")));

        assertEquals("Insufficient funds", ex.getMessage());
        assertEquals(new BigDecimal("100.00"), alice.getBalance());
        verify(accountRepository, never()).findByUsername(anyString());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmount_unknownRecipient_throwsRecipientNotFoundAndSavesNothing() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(alice, "ghost", new BigDecimal("10.00")));

        assertEquals("Recipient account not found", ex.getMessage());
        assertEquals(new BigDecimal("100.00"), alice.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmount_unknownRecipientWithInsufficientFunds_reportsInsufficientFunds() {
        // The balance check runs before the recipient lookup, so an
        // over-balance transfer to a non-existent user reports
        // "Insufficient funds" rather than "Recipient account not found".
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(alice, "ghost", new BigDecimal("500.00")));

        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).findByUsername(anyString());
    }

    @Test
    void transferAmount_negativeAmount_isCurrentlyAccepted() {
        // LIKELY BUG: a negative transfer passes the balance check and pulls
        // money from the recipient into the sender's account.
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        accountService.transferAmount(alice, "bob", new BigDecimal("-30.00"));

        assertEquals(new BigDecimal("130.00"), alice.getBalance());
        assertEquals(new BigDecimal("20.00"), bob.getBalance());
        captureTransactions(2);
    }

    // ---- registerAccount ----

    @Test
    void registerAccount_encodesPasswordAndStartsWithZeroBalance() {
        when(accountRepository.findByUsername("carol")).thenReturn(Optional.<Account>empty());
        when(passwordEncoder.encode("s3cret")).thenReturn("ENCODED");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account created = accountService.registerAccount("carol", "s3cret");

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();

        assertSame(saved, created);
        assertEquals("carol", saved.getUsername());
        assertEquals("ENCODED", saved.getPassword());
        assertEquals(0, saved.getBalance().compareTo(BigDecimal.ZERO));
        verify(passwordEncoder).encode("s3cret");
    }

    @Test
    void registerAccount_duplicateUsername_throwsAndSavesNothing() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("alice", "pw"));

        assertEquals("Username already exists", ex.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
        verify(accountRepository, never()).save(any(Account.class));
    }
}
