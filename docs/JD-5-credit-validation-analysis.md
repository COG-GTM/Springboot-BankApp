# Technical Analysis: Credit Limit Validation Security Patterns

**Ticket Reference:** JD-5 - Credit Limit Validation Flaw Allows Fraudulent Transactions

**Date:** January 23, 2026

**Purpose:** This document provides a comparative analysis of credit validation implementations in modern Java banking applications to inform the fix for the COBOL credit validation vulnerability.

---

## Executive Summary

The COBOL credit validation vulnerability (JD-5) stems from using cycle-to-date calculations instead of actual account balances for credit limit validation. This analysis examines two Java Spring Boot banking applications to identify secure validation patterns that can guide the COBOL system fix.

The flawed COBOL formula:
```cobol
WS-TEMP-BAL = ACCT-CURR-CYC-CREDIT - ACCT-CURR-CYC-DEBIT + DALYTRAN-AMT
```

This approach only considers current billing cycle activity, ignoring balances carried forward from previous cycles. An account with a $10,000 balance from previous cycles would pass validation for new transactions as long as current cycle activity remains within limits.

---

## Section 1: Java Implementation Patterns

### 1.1 Repository Analysis Overview

Two Spring Boot banking applications were analyzed:

1. **COG-GTM/Springboot-BankApp** - A full-featured banking application with Spring Security integration
2. **parkerduff/Banking-Project-Spring-Boot-JPA-REST-API-** - A REST API banking service with JPA persistence

### 1.2 Balance Validation in Springboot-BankApp

The `AccountService.java` demonstrates secure balance validation patterns:

```java
// From: src/main/java/com/example/bankapp/service/AccountService.java

public void withdraw(Account account, BigDecimal amount) {
    // CRITICAL: Validates against ACTUAL current balance, not cycle data
    if (account.getBalance().compareTo(amount) < 0) {
        throw new RuntimeException("Insufficient funds");
    }
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

**Key Security Pattern:** The validation uses `account.getBalance()` which retrieves the actual persisted balance from the database, representing the true account state including all historical transactions.

For transfers, the same validation pattern is applied:

```java
// From: src/main/java/com/example/bankapp/service/AccountService.java

public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
    // Pre-authorization check using actual balance
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

    // Create audit trail for both accounts
    // ... transaction records created
}
```

### 1.3 Balance Validation in Banking-Project-Spring-Boot-JPA-REST-API

The `BankService.java` implements similar secure patterns with explicit transactional boundaries:

```java
// From: BankService.java

@Transactional
public Transaction withdrawMoney(String accountNumber, BigDecimal amount, String description) {
    Optional<BankAccount> accountOptional = bankAccountRepository.findByAccountNumber(accountNumber);

    if (accountOptional.isPresent()) {
        BankAccount account = accountOptional.get();

        // CRITICAL: Check against actual persisted balance
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance in account: " + accountNumber);
        }

        // Update account balance atomically within transaction
        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        bankAccountRepository.save(account);

        // Create immutable transaction record
        Transaction transaction = new Transaction("WITHDRAWAL", amount, description, account);
        return transactionRepository.save(transaction);

    } else {
        throw new RuntimeException("Account not found with number: " + accountNumber);
    }
}
```

**Key Security Pattern:** The `@Transactional` annotation ensures that balance check, balance update, and transaction recording occur atomically. If any step fails, the entire operation rolls back.

### 1.4 Account Entity Design

Both implementations store balance as a single authoritative field:

```java
// From: Springboot-BankApp - Account.java
@Entity
public class Account implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private BigDecimal balance;  // Single source of truth for account balance

    @OneToMany(mappedBy = "account")
    private List<Transaction> transactions;  // Audit trail only, not used for balance calculation
}
```

```java
// From: Banking-Project - BankAccount.java
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
    private BigDecimal balance;  // Actual balance, updated with each transaction
    private LocalDateTime createdAt;
}
```

**Critical Design Decision:** The balance field represents the actual account state. Transactions are recorded for audit purposes but the balance is not derived from summing transactions. This ensures validation always uses the authoritative balance value.

---

## Section 2: Comparative Security Analysis

### 2.1 COBOL Vulnerability vs Java Implementation

| Aspect | Flawed COBOL Approach | Secure Java Approach |
|--------|----------------------|---------------------|
| **Balance Source** | Cycle-to-date calculations (ACCT-CURR-CYC-CREDIT - ACCT-CURR-CYC-DEBIT) | Actual persisted balance field |
| **Historical Data** | Ignores previous cycle balances | Includes all historical transactions |
| **Validation Timing** | During transaction processing | Before transaction execution (pre-authorization) |
| **Data Freshness** | Stale (cycle-based) | Real-time (current database state) |
| **Fraud Vector** | Accounts with prior balances can exceed limits | No bypass possible - actual balance always checked |

### 2.2 The Cycle-Based Calculation Flaw

The COBOL formula `WS-TEMP-BAL = ACCT-CURR-CYC-CREDIT - ACCT-CURR-CYC-DEBIT + DALYTRAN-AMT` creates a critical security gap:

**Scenario demonstrating the vulnerability:**

1. Account has $15,000 balance carried from previous billing cycle
2. Credit limit is $20,000
3. Current cycle: $0 credits, $0 debits
4. Customer attempts $18,000 transaction

**COBOL Calculation:**
```
WS-TEMP-BAL = $0 (cycle credits) - $0 (cycle debits) + $18,000 (new transaction)
WS-TEMP-BAL = $18,000
```
Result: Transaction APPROVED (appears within limit)

**Actual Account State:**
```
True Balance = $15,000 (prior) + $18,000 (new) = $33,000
```
Result: Account now $13,000 OVER the $20,000 credit limit

**Java Calculation (secure):**
```java
if (account.getBalance().compareTo(amount) < 0) {
    throw new RuntimeException("Insufficient funds");
}
// Balance = $15,000, checking if new transaction would exceed available credit
// For credit accounts: if (balance + amount > creditLimit) reject
```

### 2.3 Security Advantages of Actual Balance Approach

**Accuracy:** The persisted balance reflects the true account state at any point in time, incorporating all historical activity regardless of billing cycles.

**Simplicity:** A single balance field eliminates complex calculations that can introduce logic errors or be manipulated.

**Auditability:** The balance field combined with transaction history provides a complete audit trail. The balance can be verified by summing all transactions.

**Real-time Validation:** Each transaction validates against the current database state, preventing race conditions where multiple transactions could individually pass validation but collectively exceed limits.

### 2.4 Race Condition Prevention

Both Java implementations use database transactions to prevent race conditions:

```java
@Transactional
public Transaction withdrawMoney(String accountNumber, BigDecimal amount, String description) {
    // Within a transaction:
    // 1. Read current balance (with potential row lock)
    // 2. Validate against balance
    // 3. Update balance
    // 4. Record transaction
    // All steps succeed or all fail together
}
```

The `@Transactional` annotation in Spring ensures:
- Atomic execution of the entire operation
- Isolation from concurrent transactions
- Rollback on any failure

For the COBOL system, equivalent protection would require:
- Database-level locking during validation and update
- Single transaction boundary encompassing read, validate, and write operations

---

## Section 3: Recommendations for COBOL Fix

### 3.1 Primary Recommendation: Use Actual Balance

Replace the cycle-based calculation with actual account balance retrieval:

**Current (Vulnerable):**
```cobol
COMPUTE WS-TEMP-BAL = ACCT-CURR-CYC-CREDIT 
                    - ACCT-CURR-CYC-DEBIT 
                    + DALYTRAN-AMT
```

**Recommended (Secure):**
```cobol
MOVE ACCT-CURRENT-BALANCE TO WS-TEMP-BAL
ADD DALYTRAN-AMT TO WS-TEMP-BAL
```

Where `ACCT-CURRENT-BALANCE` is the actual persisted account balance that includes all historical transactions.

### 3.2 Validation Timing

Ensure validation occurs BEFORE any balance updates:

```cobol
* Pre-authorization validation
IF WS-TEMP-BAL > ACCT-CREDIT-LIMIT
    SET WS-ERR-FLG TO TRUE
    MOVE "TRANSACTION EXCEEDS CREDIT LIMIT" TO WS-MESSAGE
    PERFORM SEND-ERROR-SCREEN
    GO TO PROCESS-EXIT
END-IF

* Only proceed with transaction if validation passes
PERFORM UPDATE-ACCOUNT-BALANCE
PERFORM RECORD-TRANSACTION
```

### 3.3 Atomic Transaction Processing

Implement transaction boundaries to prevent partial updates:

```cobol
* Begin transaction scope
EXEC SQL
    BEGIN TRANSACTION
END-EXEC

* Perform validation and updates
PERFORM VALIDATE-CREDIT-LIMIT
IF WS-ERR-FLG
    EXEC SQL ROLLBACK END-EXEC
    GO TO PROCESS-EXIT
END-IF

PERFORM UPDATE-ACCOUNT-BALANCE
PERFORM RECORD-TRANSACTION-HISTORY

* Commit only if all operations succeed
EXEC SQL COMMIT END-EXEC
```

### 3.4 Additional Security Considerations

**Balance Reconciliation:** Implement periodic reconciliation between the balance field and the sum of all transactions to detect any discrepancies:

```cobol
* Reconciliation check
COMPUTE WS-CALC-BALANCE = 
    FUNCTION SUM(TRANSACTION-AMOUNTS)
IF WS-CALC-BALANCE NOT = ACCT-CURRENT-BALANCE
    PERFORM LOG-DISCREPANCY
    PERFORM ALERT-OPERATIONS
END-IF
```

**Audit Trail:** Ensure every transaction that modifies balance creates an immutable audit record:

```cobol
* Create audit record before balance update
MOVE ACCT-CURRENT-BALANCE TO AUDIT-PREV-BALANCE
MOVE WS-TEMP-BAL TO AUDIT-NEW-BALANCE
MOVE DALYTRAN-AMT TO AUDIT-TRANSACTION-AMT
MOVE CURRENT-TIMESTAMP TO AUDIT-TIMESTAMP
PERFORM WRITE-AUDIT-RECORD
```

**Concurrent Access Protection:** Use database locking to prevent race conditions:

```cobol
* Lock account record during update
EXEC SQL
    SELECT CURRENT_BALANCE 
    INTO :WS-CURRENT-BAL
    FROM ACCOUNTS
    WHERE ACCOUNT_NUMBER = :WS-ACCT-NUM
    FOR UPDATE
END-EXEC
```

### 3.5 Implementation Checklist

1. **Identify Balance Field:** Locate or create a field that stores the actual account balance (not cycle-based)

2. **Update Validation Logic:** Replace cycle-based calculation with actual balance retrieval

3. **Add Transaction Boundaries:** Ensure validation and update occur within a single transaction

4. **Implement Locking:** Add row-level locking to prevent concurrent modification

5. **Create Audit Trail:** Log all balance changes with before/after values

6. **Add Reconciliation:** Implement periodic balance verification

7. **Test Edge Cases:**
   - Transactions at cycle boundaries
   - Concurrent transaction attempts
   - Accounts with large prior balances
   - Transactions exactly at credit limit

---

## Section 4: Security Insights from Java Implementations

### 4.1 Defense in Depth

Both Java applications implement multiple layers of validation:

**Controller Layer:** Basic input validation and error handling
```java
@PostMapping("/accounts/withdraw")
public ResponseEntity<?> withdrawMoney(...) {
    try {
        Transaction transaction = bankService.withdrawMoney(accountNumber, amount, description);
        return ResponseEntity.ok(transaction);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error during withdrawal: " + e.getMessage());
    }
}
```

**Service Layer:** Business logic validation (balance checks)
```java
if (account.getBalance().compareTo(amount) < 0) {
    throw new RuntimeException("Insufficient funds");
}
```

**Database Layer:** Constraints and transaction isolation

### 4.2 Use of BigDecimal for Financial Calculations

Both implementations use `BigDecimal` instead of floating-point types:

```java
private BigDecimal balance;

// Comparison
if (account.getBalance().compareTo(amount) < 0)

// Arithmetic
BigDecimal newBalance = account.getBalance().subtract(amount);
```

This prevents floating-point precision errors that could lead to incorrect balance calculations. The COBOL equivalent should use appropriate DECIMAL or PACKED-DECIMAL fields with sufficient precision.

### 4.3 Immutable Transaction Records

Transactions are created as new records, never modified:

```java
Transaction transaction = new Transaction("WITHDRAWAL", amount, description, account);
return transactionRepository.save(transaction);
```

This creates an immutable audit trail. The COBOL system should similarly ensure transaction records cannot be modified after creation.

### 4.4 Authentication Integration

The Springboot-BankApp integrates balance operations with authentication:

```java
String username = SecurityContextHolder.getContext().getAuthentication().getName();
Account account = accountService.findAccountByUsername(username);
```

This ensures users can only access their own accounts. The COBOL system should verify account ownership before processing transactions.

---

## Conclusion

The Java banking implementations demonstrate that secure credit validation requires using actual persisted balances rather than derived calculations. The key principles are:

1. **Single Source of Truth:** Maintain one authoritative balance field
2. **Pre-Authorization Validation:** Check limits before processing
3. **Atomic Transactions:** Ensure all-or-nothing execution
4. **Immutable Audit Trail:** Record all changes permanently
5. **Concurrent Access Control:** Prevent race conditions with locking

Applying these patterns to the COBOL system will eliminate the vulnerability that allows fraudulent transactions when accounts have significant balances from previous cycles.

---

## References

- COG-GTM/Springboot-BankApp: `src/main/java/com/example/bankapp/service/AccountService.java`
- parkerduff/Banking-Project-Spring-Boot-JPA-REST-API-: `BankService.java`
- JD-5: Credit Limit Validation Flaw Allows Fraudulent Transactions
