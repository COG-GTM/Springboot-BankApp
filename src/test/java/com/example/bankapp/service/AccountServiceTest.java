package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    // Verifies that depositing funds increases the account balance and persists
    // both the updated account and a "Deposit" transaction record.
    @Test
    void deposit_increasesBalanceAndSavesTransaction() {
        Account account = new Account();
        account.setUsername("user1");
        account.setBalance(BigDecimal.valueOf(100));

        accountService.deposit(account, BigDecimal.valueOf(50));

        assertEquals(BigDecimal.valueOf(150), account.getBalance());
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertEquals("Deposit", txCaptor.getValue().getType());
    }

    // Verifies that withdrawing funds decreases the account balance and persists
    // both the updated account and a withdrawal transaction record.
    @Test
    void withdraw_decreasesBalanceAndSavesTransaction() {
        Account account = new Account();
        account.setUsername("user1");
        account.setBalance(BigDecimal.valueOf(100));

        accountService.withdraw(account, BigDecimal.valueOf(30));

        assertEquals(BigDecimal.valueOf(70), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    // Verifies that withdrawing more than the available balance throws a
    // RuntimeException with "Insufficient funds" and does not save anything.
    @Test
    void withdraw_insufficientFunds_throwsException() {
        Account account = new Account();
        account.setBalance(BigDecimal.valueOf(10));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, BigDecimal.valueOf(50)));

        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any());
    }

    // Verifies that registering a new account encodes the password, sets the
    // balance to zero, and saves the account with the correct username.
    @Test
    void registerAccount_success() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.registerAccount("newuser", "pass");

        ArgumentCaptor<Account> acctCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(acctCaptor.capture());
        Account saved = acctCaptor.getValue();

        assertEquals("newuser", saved.getUsername());
        assertEquals("encodedPass", saved.getPassword());
        assertEquals(BigDecimal.ZERO, saved.getBalance());
    }

    // Verifies that registering with an already-taken username throws a
    // RuntimeException with "Username already exists".
    @Test
    void registerAccount_duplicateUsername_throwsException() {
        Account existing = new Account();
        existing.setUsername("existing");
        when(accountRepository.findByUsername("existing")).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("existing", "pass"));

        assertEquals("Username already exists", ex.getMessage());
    }

    // Verifies a successful transfer: sender balance decreases, recipient balance
    // increases, and both a debit and credit transaction are saved.
    @Test
    void transferAmount_success() {
        Account fromAccount = new Account();
        fromAccount.setUsername("sender");
        fromAccount.setBalance(BigDecimal.valueOf(200));

        Account toAccount = new Account();
        toAccount.setUsername("recipient");
        toAccount.setBalance(BigDecimal.valueOf(50));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(toAccount));

        accountService.transferAmount(fromAccount, "recipient", BigDecimal.valueOf(75));

        assertEquals(BigDecimal.valueOf(125), fromAccount.getBalance());
        assertEquals(BigDecimal.valueOf(125), toAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());

        Transaction debit = txCaptor.getAllValues().get(0);
        Transaction credit = txCaptor.getAllValues().get(1);
        assertEquals("Transfer Out to recipient", debit.getType());
        assertEquals("Transfer In from sender", credit.getType());
    }

    // Verifies that transferring more than the sender's balance throws a
    // RuntimeException with "Insufficient funds".
    @Test
    void transferAmount_insufficientFunds_throwsException() {
        Account fromAccount = new Account();
        fromAccount.setUsername("sender");
        fromAccount.setBalance(BigDecimal.valueOf(10));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "recipient", BigDecimal.valueOf(50)));

        assertEquals("Insufficient funds", ex.getMessage());
    }
}
