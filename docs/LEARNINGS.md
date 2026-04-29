# Learnings: Async Payment Migration & Transactional Safety

This document captures key technical insights from migrating the BankApp's synchronous payment flows to async patterns while maintaining transactional integrity.

---

## 1. Spring @Async Internals

### How Spring Proxies Async Methods

Spring's `@Async` works through AOP proxying. When a bean method is annotated with `@Async`, Spring wraps the bean in a proxy that intercepts calls and submits them to a `TaskExecutor` instead of executing on the caller's thread. The actual method runs on a thread from the configured executor pool.

**Critical gotcha — self-invocation doesn't work with @Async.** If `ServiceA.methodA()` calls `this.methodB()` where `methodB()` is `@Async`, the call bypasses the proxy and executes synchronously. This is because `this` refers to the raw bean, not the proxy. The solution is to inject the service into itself or extract async methods into a separate service (which is what we did with `TransactionLoggingService`).

### TaskExecutor Thread Pool Management

The `ThreadPoolTaskExecutor` configured in `AsyncConfig` manages a pool with:
- **Core pool size (5):** Threads kept alive even when idle
- **Max pool size (10):** Upper bound when queue is full
- **Queue capacity (25):** Buffered tasks before spawning beyond core

Thread naming via `setThreadNamePrefix("payment-async-")` is critical for debugging — it lets you trace in logs which operations are running asynchronously vs. synchronously. When we see log lines from the `payment-async-1` thread, we know the transaction logging is correctly offloaded.

---

## 2. @Transactional + @Async Interaction

### Why You Cannot Combine Both on the Same Method

Spring's `@Transactional` binds a transaction to the **current thread** via `ThreadLocal`. When `@Async` moves execution to a different thread, the transaction context does **not** propagate. This means:

```
@Transactional
@Async
public void doWork() {
    // The @Transactional has NO effect here because @Async 
    // runs this on a new thread with no transaction context
    repository.save(entity); // runs without transactional protection
}
```

If both annotations are on the same method, one of two things happens depending on proxy ordering:
1. If `@Async` is processed first, the method runs on a new thread without the caller's transaction
2. If `@Transactional` is processed first, it creates a transaction on the new async thread — but this is a **separate** transaction from the caller, defeating the purpose of atomicity

### The Pattern: Sync Writes + Async Audit

The correct architecture separates concerns:
- **Critical path (balance updates):** Synchronous + `@Transactional` in `AccountService`. The `deposit()`, `withdraw()`, and `transferAmount()` methods mutate balances and save accounts within a single transaction.
- **Audit trail (transaction logging):** Asynchronous in `TransactionLoggingService`. Creating `Transaction` records is non-critical and can be offloaded to the thread pool. If async logging fails, the balance is still correct.

This separation means a failed transaction log doesn't roll back a successful balance update, which is the correct business behavior for a banking application — the balance is the source of truth, and the audit trail is eventually consistent.

---

## 3. CompletableFuture Patterns

### Composing Async Results

`CompletableFuture` enables non-blocking composition:

```java
// Sequential composition
CompletableFuture<Transaction> result = logDepositAsync(account, amount)
    .thenApply(tx -> { tx.setStatus(COMPLETED); return tx; });

// Parallel composition
CompletableFuture.allOf(
    logTransferAsync(sender, amount, "Transfer Out"),
    logTransferAsync(recipient, amount, "Transfer In")
).join();
```

### Exception Handling in Async Chains

Exceptions in `CompletableFuture.supplyAsync()` are wrapped in `CompletionException`. In the REST controller, we catch `RuntimeException` inside the supplier to return proper HTTP error responses:

```java
CompletableFuture.supplyAsync(() -> {
    try {
        accountService.withdraw(account, amount);
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
    return ResponseEntity.ok(response);
}, paymentTaskExecutor);
```

### Testing Async Code

Key testing insight: `@Transactional` test annotations don't work with async endpoints because the test transaction lives on the test thread, while the async handler runs on the executor thread (which can't see uncommitted data). The fix is to **not** use `@Transactional` on async integration tests and instead use explicit `@BeforeEach`/`@AfterEach` cleanup.

For MockMvc async tests, the pattern is:
```java
MvcResult result = mockMvc.perform(post("/api/deposit").param("amount", "500"))
    .andExpect(request().asyncStarted())
    .andReturn();

mockMvc.perform(asyncDispatch(result))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.newBalance").value(1500.00));
```

---

## 4. Race Conditions in Financial Systems

### The Lost Update Problem

Without `@Transactional`, the `transferAmount()` method performed 4 separate DB writes:
1. Debit sender account
2. Save sender
3. Credit recipient account
4. Save recipient

If two concurrent transfers happen simultaneously, both could read the same initial balance, compute new balances independently, and write back — causing one transfer to be "lost." This is the classic lost update anomaly.

### How @Transactional Prevents Lost Updates

Adding `@Transactional` wraps all 4 operations in a single database transaction. With the default isolation level (`READ_COMMITTED` on most databases), this provides:
- **Atomicity:** Either all 4 writes succeed or none do. If saving the recipient fails, the sender's debit is rolled back.
- **Row-level locking:** When Hibernate loads and modifies an account, the row is locked until the transaction commits, preventing concurrent modifications.

For higher-contention scenarios, you could escalate to `SERIALIZABLE` isolation or use optimistic locking with `@Version`, but `READ_COMMITTED` + `@Transactional` is sufficient for this application's concurrency profile.

### SecurityContext and Thread Boundaries

A subtle but critical finding: `SecurityContextHolder` stores the authentication context in a `ThreadLocal`. When using `CompletableFuture.supplyAsync()`, the async thread does **not** inherit the security context. The solution is to capture the username synchronously on the HTTP request thread and pass it as a closure variable to the async supplier:

```java
// Capture on request thread (has SecurityContext)
String username = SecurityContextHolder.getContext().getAuthentication().getName();

// Use captured value on async thread (no SecurityContext)
return CompletableFuture.supplyAsync(() -> {
    Account account = accountService.findAccountByUsername(username);
    // ...
}, paymentTaskExecutor);
```

### H2 Reserved Keywords

The entity name `Transaction` maps to a table name `transaction`, which is a SQL reserved keyword. H2 in test mode rejects this. Renaming the table via `@Table(name = "bank_transaction")` resolves the issue without affecting MySQL in production (where `transaction` may be quoted automatically by the dialect).
