# Technical Analysis: Credit Limit Validation Security Patterns

**Ticket Reference:** JD-5 - Credit Limit Validation Flaw Allows Fraudulent Transactions

**Analysis Date:** January 23, 2026

**Repositories Analyzed:**
- COG-GTM/Springboot-BankApp
- parkerduff/Banking-Project-Spring-Boot-JPA-REST-API-

## Executive Summary

This document provides a comparative analysis of credit validation implementations in modern Java banking applications to inform the remediation of the COBOL credit limit validation vulnerability documented in ticket JD-5. The flawed COBOL implementation uses cycle-to-date calculations instead of actual account balances, allowing fraudulent transactions when accounts have significant balances from previous cycles. The Java implementations examined demonstrate secure patterns that validate against actual current balances before transaction posting.

## Section 1: Java Implementation Patterns

### 1.1 Balance Validation in parkerduff/Banking-Project-Spring-Boot-JPA-REST-API-

The BankService class implements balance validation that directly addresses the vulnerability pattern seen in the COBOL system. The withdrawal method validates against the actual current balance before any transaction processing occurs.

**Withdrawal Validation (BankService.java, lines 62-86):**

```java
@Transactional
public Transaction withdrawMoney(String accountNumber, BigDecimal amount, String description) {
    Optional<BankAccount> accountOptional = bankAccountRepository.findByAccountNumber(accountNumber);

    if (accountOptional.isPresent()) {
        BankAccount account = accountOptional.get();

        // Check sufficient balance - uses ACTUAL current balance
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance in account: " + accountNumber);
        }

        // Update account balance only AFTER validation passes
        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        bankAccountRepository.save(account);

        // Create transaction record
        Transaction transaction = new Transaction("WITHDRAWAL", amount, description, account);
        return transactionRepository.save(transaction);

    } else {
        throw new RuntimeException("Account not found with number: " + accountNumber);
    }
}
```

**Key Security Pattern:** The validation uses `account.getBalance()` which retrieves the actual persisted balance from the database, not a calculated value based on cycle activity. This ensures the validation considers all historical transactions, not just current cycle data.

**Transfer Validation (BankService.java, lines 94-107):**

```java
@Transactional
public Transaction transferMoney(String fromAccountNumber, String toAccountNumber,
                                 BigDecimal amount, String description) {

    // Withdraw from source account - inherits balance validation
    Transaction withdrawalTransaction = withdrawMoney(fromAccountNumber, amount,
            "Transfer to account: " + toAccountNumber);

    // Deposit to target account
    depositMoney(toAccountNumber, amount,
            "Transfer from account: " + fromAccountNumber);

    return withdrawalTransaction;
}
```

The transfer operation reuses the withdrawal validation, ensuring consistent security enforcement across all debit operations.

### 1.2 Balance Validation in COG-GTM/Springboot-BankApp

The AccountService class in this repository follows the same secure pattern, validating against actual balances before processing.

**Withdrawal Validation (AccountService.java, lines 64-78):**

```java
public void withdraw(Account account, BigDecimal amount) {
    // Validate against ACTUAL current balance before any modification
    if (account.getBalance().compareTo(amount) < 0) {
        throw new RuntimeException("Insufficient funds");
    }
    
    // Balance modification occurs only after validation
    account.setBalance(account.getBalance().subtract(amount));
    accountRepository.save(account);

    Transaction transaction = new Transaction(
            amount,
            "Withdrawal",
            LocalDateTime.now(),
            account
    );
    transactionRepository.save(transaction);
}
```

**Transfer Validation (AccountService.java, lines 103-135):**

```java
public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
    // Pre-validation against actual balance
    if (fromAccount.getBalance().compareTo(amount) < 0) {
        throw new RuntimeException("Insufficient funds");
    }

    Account toAccount = accountRepository.findByUsername(toUsername)
            .orElseThrow(() -> new RuntimeException("Recipient account not found"));

    // Deduct from sender's account
    fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
    accountRepository.save(fromAccount);

    // Add to recipient's account
    toAccount.setBalance(toAccount.getBalance().add(amount));
    accountRepository.save(toAccount);

    // Create transaction records for both accounts
    Transaction debitTransaction = new Transaction(
            amount,
            "Transfer Out to " + toAccount.getUsername(),
            LocalDateTime.now(),
            fromAccount
    );
    transactionRepository.save(debitTransaction);

    Transaction creditTransaction = new Transaction(
            amount,
            "Transfer In from " + fromAccount.getUsername(),
            LocalDateTime.now(),
            toAccount
    );
    transactionRepository.save(creditTransaction);
}
```

### 1.3 Account Entity Design

Both implementations store the actual balance as a persistent field in the Account entity, not as a calculated value.

**parkerduff/Banking-Project (BankAccount.java):**

```java
@Entity
@Table(name = "bank_accounts")
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String accountNumber;

    private String accountHolderName;
    private String accountType;
    private BigDecimal balance;  // Actual persisted balance
    private LocalDateTime createdAt;
    
    // ...
}
```

**COG-GTM/Springboot-BankApp (Account.java):**

```java
@Entity
public class Account implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private BigDecimal balance;  // Actual persisted balance

    @OneToMany(mappedBy = "account")
    private List<Transaction> transactions;
    
    // ...
}
```

## Section 2: Comparative Security Analysis

### 2.1 The COBOL Vulnerability

The flawed COBOL implementation calculates available balance using:

```cobol
WS-TEMP-BAL = ACCT-CURR-CYC-CREDIT - ACCT-CURR-CYC-DEBIT + DALYTRAN-AMT
```

This formula has critical security flaws:

| Issue | Description | Risk |
|-------|-------------|------|
| Cycle-Based Calculation | Only considers current billing cycle activity | Ignores balances from previous cycles |
| Missing Historical Data | Does not account for carried-over balances | Allows transactions that exceed true available credit |
| Timing Vulnerability | Cycle reset creates validation gap | Fraudulent transactions possible at cycle boundaries |

**Attack Scenario:** An account with a $10,000 balance from previous cycles but only $500 in current cycle debits would pass validation for a $9,000 transaction because the formula only sees the $500 cycle-to-date debit, not the actual $10,000 outstanding balance.

### 2.2 How Java Implementations Avoid This Vulnerability

The Java implementations avoid the COBOL vulnerability through several key design decisions:

**Actual Balance Storage:** Both Java implementations store the actual current balance as a persistent field (`balance`) that is updated with every transaction. This eliminates the need for cycle-based calculations.

**Pre-Transaction Validation:** Validation occurs before any balance modification:

```java
// Java pattern - validate BEFORE modifying
if (account.getBalance().compareTo(amount) < 0) {
    throw new RuntimeException("Insufficient funds");
}
// Only then modify balance
account.setBalance(account.getBalance().subtract(amount));
```

**No Cycle Dependency:** The Java implementations have no concept of billing cycles in their validation logic. Every transaction is validated against the true current state of the account.

### 2.3 Security Advantages of Actual Balance vs. Cycle Calculations

| Aspect | COBOL Cycle-Based | Java Actual Balance |
|--------|-------------------|---------------------|
| Data Source | Calculated from cycle activity | Persisted actual value |
| Historical Accuracy | Ignores previous cycles | Includes all history |
| Validation Timing | Post-calculation | Pre-transaction |
| Cycle Boundary Risk | Vulnerable at reset | No cycle dependency |
| Audit Trail | Requires reconstruction | Direct balance history |
| Complexity | Multiple data points | Single source of truth |

### 2.4 Transaction Atomicity

Both Java implementations use Spring's `@Transactional` annotation to ensure atomic operations:

```java
@Transactional
public Transaction withdrawMoney(String accountNumber, BigDecimal amount, String description) {
    // All operations within this method are atomic
    // If any operation fails, all changes are rolled back
}
```

This prevents partial transaction states where a balance might be debited but the transaction record not created, or vice versa.

## Section 3: Recommendations for COBOL Fix

### 3.1 Primary Recommendation: Use Actual Balance

The COBOL validation should be modified to use the actual account balance rather than cycle-to-date calculations. The corrected formula should be:

```cobol
* CORRECTED VALIDATION
WS-TEMP-BAL = ACCT-ACTUAL-BALANCE + DALYTRAN-AMT
```

Where `ACCT-ACTUAL-BALANCE` represents the true current balance of the account, including all historical activity.

### 3.2 Validation Timing

Following the Java pattern, validation should occur in the PROCESS-ENTER-KEY section before any file writes:

```cobol
PROCESS-ENTER-KEY.
    * Validate against actual balance BEFORE processing
    IF DALYTRAN-AMT > ACCT-ACTUAL-BALANCE
        SET WS-ERR-FLG TO TRUE
        MOVE 'INSUFFICIENT FUNDS - TRANSACTION DECLINED' 
            TO WS-MESSAGE
        MOVE -1 TO DALYTRAN-AMT-L
        PERFORM SEND-SCREEN
        GO TO PROCESS-ENTER-KEY-EXIT
    END-IF.
    
    * Only proceed with transaction if validation passes
    PERFORM PROCESS-TRANSACTION.
```

### 3.3 Data Source Recommendations

The validation should retrieve the actual balance from the master account record, not from cycle summary fields:

| Current (Vulnerable) | Recommended (Secure) |
|---------------------|---------------------|
| ACCT-CURR-CYC-CREDIT | ACCT-ACTUAL-BALANCE |
| ACCT-CURR-CYC-DEBIT | (not needed) |
| Calculated WS-TEMP-BAL | Direct balance comparison |

### 3.4 Additional Security Considerations

Based on patterns observed in the Java implementations, the following additional security measures should be considered for the COBOL fix:

**Atomic Transaction Processing:** Ensure that balance updates and transaction record creation occur as an atomic unit. If using CICS, consider using SYNCPOINT to ensure transactional integrity.

**Audit Trail:** Create immutable transaction records for every operation, similar to the Java Transaction entity pattern. Each record should capture the transaction amount, type, timestamp, and resulting balance.

**Error Handling:** Implement clear error messages that do not expose internal system details but provide actionable information to users:

```cobol
* Good error handling pattern
EVALUATE TRUE
    WHEN DALYTRAN-AMT > ACCT-ACTUAL-BALANCE
        MOVE 'INSUFFICIENT FUNDS' TO WS-MESSAGE
    WHEN ACCT-STATUS NOT = 'ACTIVE'
        MOVE 'ACCOUNT NOT AVAILABLE' TO WS-MESSAGE
    WHEN OTHER
        MOVE 'TRANSACTION CANNOT BE PROCESSED' TO WS-MESSAGE
END-EVALUATE.
```

**Concurrent Access Protection:** Consider implementing record locking or optimistic concurrency control to prevent race conditions where multiple transactions might be validated against the same balance simultaneously.

### 3.5 Implementation Checklist

The following checklist summarizes the key changes needed to remediate the COBOL vulnerability:

1. Replace cycle-based balance calculation with actual balance retrieval
2. Move validation to occur before any balance modification
3. Implement proper error handling with WS-ERR-FLG pattern
4. Ensure atomic transaction processing
5. Create audit records for all transactions
6. Test validation at cycle boundaries to confirm fix
7. Verify concurrent transaction handling

## Section 4: Security Insights from Java Implementations

### 4.1 BigDecimal for Financial Precision

Both Java implementations use `BigDecimal` for all monetary values rather than floating-point types:

```java
private BigDecimal balance;
```

This prevents rounding errors that could accumulate over many transactions. The COBOL equivalent would be using packed decimal (COMP-3) or display numeric fields with explicit decimal positions.

### 4.2 Repository Pattern for Data Access

The Java implementations use the Repository pattern to abstract data access:

```java
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findByAccountNumber(String accountNumber);
}
```

This pattern ensures consistent data retrieval and prevents direct database manipulation that might bypass validation logic.

### 4.3 Service Layer Encapsulation

All business logic, including validation, is encapsulated in the Service layer:

```java
@Service
public class BankService {
    // All validation and business logic here
}
```

This ensures that validation cannot be bypassed by calling lower-level data access methods directly. The COBOL equivalent would be ensuring all transaction processing goes through a single validation paragraph.

### 4.4 Authentication Integration

The COG-GTM/Springboot-BankApp implementation integrates account validation with Spring Security:

```java
@Service
public class AccountService implements UserDetailsService {
    // Account operations require authenticated user context
}
```

This ensures that transaction operations are always performed in an authenticated context, preventing unauthorized access.

## Conclusion

The Java banking implementations examined demonstrate secure credit validation patterns that directly address the vulnerability in the COBOL system. The key insight is that validation must use actual current balances rather than cycle-based calculations. By adopting the patterns documented in this analysis, specifically pre-transaction validation against actual balances with atomic transaction processing, the COBOL system can be remediated to prevent the fraudulent transaction scenario described in ticket JD-5.

The recommended fix involves replacing the cycle-based calculation formula with direct actual balance comparison, implementing validation before transaction processing, and ensuring atomic operations with proper audit trails. These changes align with modern banking application security practices while remaining implementable within COBOL/CICS constraints.
