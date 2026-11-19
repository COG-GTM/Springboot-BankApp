package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryJpaTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManager entityManager;

    private Account persistAccount(String username, String balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("pw-" + username);
        account.setBalance(new BigDecimal(balance));
        return accountRepository.save(account);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void saveAssignsGeneratedIdAndFindByUsernameRoundTrips() {
        Account saved = persistAccount("alice", "12.34");
        flushAndClear();

        assertThat(saved.getId()).isNotNull();
        Account found = accountRepository.findByUsername("alice").orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getPassword()).isEqualTo("pw-alice");
        assertThat(found.getBalance()).isEqualByComparingTo("12.34");
        assertThat(found.getAuthorities()).isNull();
    }

    @Test
    void findByUsernameIsEmptyForUnknownUser() {
        persistAccount("alice", "0");

        assertThat(accountRepository.findByUsername("bob")).isEmpty();
        assertThat(accountRepository.findByUsername("ALICE")).isEmpty();
    }

    @Test
    void balanceIsStoredWithTwoDecimalScale() {
        persistAccount("alice", "10.555");
        flushAndClear();

        assertThat(accountRepository.findByUsername("alice").orElseThrow().getBalance())
                .isEqualTo(new BigDecimal("10.56"));
    }

    @Test
    void duplicateUsernamesAreNotPreventedByTheSchema() {
        persistAccount("alice", "1");
        persistAccount("alice", "2");
        flushAndClear();

        assertThat(accountRepository.count()).isEqualTo(2);
        assertThatThrownBy(() -> accountRepository.findByUsername("alice"))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }

    @Test
    void findByAccountIdReturnsOnlyThatAccountsTransactionsInInsertOrder() {
        Account alice = persistAccount("alice", "0");
        Account bob = persistAccount("bob", "0");
        LocalDateTime now = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
        transactionRepository.save(new Transaction(new BigDecimal("1.00"), "Deposit", now, alice));
        transactionRepository.save(new Transaction(new BigDecimal("2.00"), "Deposit", now, bob));
        transactionRepository.save(new Transaction(new BigDecimal("3.00"), "Withdrawal", now.plusMinutes(1), alice));
        flushAndClear();

        List<Transaction> history = transactionRepository.findByAccountId(alice.getId());

        assertThat(history).extracting(Transaction::getType).containsExactly("Deposit", "Withdrawal");
        assertThat(history).extracting(Transaction::getAmount)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1.00"), new BigDecimal("3.00"));
        assertThat(history.get(0).getTimestamp()).isEqualTo(now);
        assertThat(history.get(0).getAccount().getUsername()).isEqualTo("alice");
        assertThat(transactionRepository.findByAccountId(-1L)).isEmpty();
    }

    @Test
    void accountTransactionsCollectionIsMappedByAccount() {
        Account alice = persistAccount("alice", "0");
        transactionRepository.save(new Transaction(BigDecimal.TEN, "Deposit", LocalDateTime.now(), alice));
        flushAndClear();

        Account reloaded = accountRepository.findById(alice.getId()).orElseThrow();
        assertThat(reloaded.getTransactions()).hasSize(1);
        assertThat(reloaded.getTransactions().get(0).getType()).isEqualTo("Deposit");
    }
}
