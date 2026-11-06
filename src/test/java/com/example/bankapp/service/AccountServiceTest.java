package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account alice;

    @BeforeEach
    void setUp() {
        alice = new Account("alice", "encoded", new BigDecimal("500.00"), new ArrayList<>(), List.of());
        alice.setId(1L);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundExceptionForUnknownUser() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void unknownUserLoginRunsPasswordHashLikeWrongPasswordLogin() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(accountService);

        assertThatThrownBy(() -> provider.authenticate(
                new UsernamePasswordAuthenticationToken("alice", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> provider.authenticate(
                new UsernamePasswordAuthenticationToken("ghost", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);

        verify(passwordEncoder, times(2)).matches(any(), any());
    }

    @Test
    void registerExistingUsernameHashesPasswordAndDoesNotSave() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> accountService.registerAccount("alice", "new-password"))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(passwordEncoder).encode("new-password");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void registerNewUsernameSavesEncodedPassword() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account saved = accountService.registerAccount("bob", "secret");

        assertThat(saved.getUsername()).isEqualTo("bob");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void transferToUnknownRecipientFailsWithGenericMessageAndMovesNoMoney() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transferAmount(alice, "ghost", new BigDecimal("10")))
                .hasMessage(AccountService.TRANSFER_FAILED_MESSAGE);

        assertThat(alice.getBalance()).isEqualByComparingTo("500.00");
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferOfZeroOrNegativeAmountFailsBeforeRecipientLookup() {
        assertThatThrownBy(() -> accountService.transferAmount(alice, "bob", BigDecimal.ZERO))
                .hasMessage(AccountService.TRANSFER_FAILED_MESSAGE);
        assertThatThrownBy(() -> accountService.transferAmount(alice, "bob", new BigDecimal("-5")))
                .hasMessage(AccountService.TRANSFER_FAILED_MESSAGE);

        verify(accountRepository, never()).findByUsername(anyString());
        verify(accountRepository, never()).save(any());
    }
}
