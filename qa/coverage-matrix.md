# Coverage matrix: qa/manual-test-plan.csv → Playwright

All 60 manual rows are accounted for: **54 automated**, **6 not automated** (reasons below). Automated tests live in `e2e/tests/` and run in CI (`.github/workflows/e2e.yml`) against the real app on a MySQL 8 service container. Each test title starts with its plan ID. `node e2e/scripts/check-coverage.mjs` (also run in CI) fails if any plan row is missing from this table, if an Automated row has no matching test, or if a Not automated row has no reason.

Run locally: `demo/start-demo.sh` then `cd e2e && npm ci && npx playwright test`. Tests create their own unique customers, so they need no fixtures and can run in parallel.

| Area | Rows | Automated | Not automated |
|---|---|---|---|
| Authentication | 13 | 13 | 0 |
| Dashboard | 8 | 7 | 1 |
| Deposit | 7 | 7 | 0 |
| Withdraw | 7 | 7 | 0 |
| Transfer | 13 | 11 | 2 |
| Transactions | 7 | 6 | 1 |
| Cross-cutting | 5 | 3 | 2 |
| **Total** | **60** | **54** | **6** |

## All rows

| ID | Area | Priority | Title | Status | Playwright test | Notes / reason not automated |
|---|---|---|---|---|---|---|
| MT-001 | Authentication | High | Login page shows username and password fields | Automated | `e2e/tests/authentication.spec.ts` › MT-001 Login page shows username and password fields |  |
| MT-002 | Authentication | Medium | Register link on login page opens registration | Automated | `e2e/tests/authentication.spec.ts` › MT-002 Register link on login page opens registration |  |
| MT-003 | Authentication | Low | Login link on register page returns to login | Automated | `e2e/tests/authentication.spec.ts` › MT-003 Login link on register page returns to login |  |
| MT-004 | Authentication | High | New customer can register | Automated | `e2e/tests/authentication.spec.ts` › MT-004 New customer can register |  |
| MT-005 | Authentication | High | Duplicate username is rejected | Automated | `e2e/tests/authentication.spec.ts` › MT-005 Duplicate username is rejected |  |
| MT-006 | Authentication | High | Valid login opens dashboard | Automated | `e2e/tests/authentication.spec.ts` › MT-006 Valid login opens dashboard |  |
| MT-007 | Authentication | High | Wrong password is rejected | Automated | `e2e/tests/authentication.spec.ts` › MT-007 Wrong password is rejected |  |
| MT-008 | Authentication | High | Unknown username gets the same error | Automated | `e2e/tests/authentication.spec.ts` › MT-008 Unknown username gets the same error | Asserts the unknown-user message is identical to the wrong-password message. |
| MT-009 | Authentication | Medium | Password is case sensitive | Automated | `e2e/tests/authentication.spec.ts` › MT-009 Password is case sensitive |  |
| MT-010 | Authentication | High | Dashboard requires login | Automated | `e2e/tests/authentication.spec.ts` › MT-010 Dashboard requires login |  |
| MT-011 | Authentication | High | Transaction history requires login | Automated | `e2e/tests/authentication.spec.ts` › MT-011 Transaction history requires login |  |
| MT-012 | Authentication | High | Logout returns to login | Automated | `e2e/tests/authentication.spec.ts` › MT-012 Logout returns to login |  |
| MT-013 | Authentication | High | Session ends after logout | Automated | `e2e/tests/authentication.spec.ts` › MT-013 Session ends after logout |  |
| MT-014 | Dashboard | High | New customer starts at zero | Automated | `e2e/tests/dashboard.spec.ts` › MT-014 New customer starts at zero |  |
| MT-015 | Dashboard | Low | Welcome message shows the customer | Automated | `e2e/tests/dashboard.spec.ts` › MT-015 Welcome message shows the customer |  |
| MT-016 | Dashboard | Medium | Account number is shown | Automated | `e2e/tests/dashboard.spec.ts` › MT-016 Account number is shown |  |
| MT-017 | Dashboard | Low | Navigation links are present | Automated | `e2e/tests/dashboard.spec.ts` › MT-017 Navigation links are present |  |
| MT-018 | Dashboard | Medium | Deposit panel opens | Automated | `e2e/tests/dashboard.spec.ts` › MT-018 Deposit panel opens |  |
| MT-019 | Dashboard | Medium | Withdraw panel opens | Automated | `e2e/tests/dashboard.spec.ts` › MT-019 Withdraw panel opens |  |
| MT-020 | Dashboard | Medium | Transfer panel opens | Automated | `e2e/tests/dashboard.spec.ts` › MT-020 Transfer panel opens |  |
| MT-021 | Dashboard | Low | Brand colours and logo match the brand guide | Not automated | — | Expected result is a human sign-off ("Brand team signs off") against a brand guide that is not in the repo. There is no machine-checkable pass condition; inventing pixel/colour assertions would not be the brand team's judgment. |
| MT-022 | Deposit | High | Deposit increases balance | Automated | `e2e/tests/deposit.spec.ts` › MT-022 Deposit increases balance |  |
| MT-023 | Deposit | High | Deposits add up | Automated | `e2e/tests/deposit.spec.ts` › MT-023 Deposits add up |  |
| MT-024 | Deposit | High | Deposit appears in history | Automated | `e2e/tests/deposit.spec.ts` › MT-024 Deposit appears in history |  |
| MT-025 | Deposit | Medium | Empty deposit is blocked | Automated | `e2e/tests/deposit.spec.ts` › MT-025 Empty deposit is blocked | Proves no POST /deposit was sent (HTML5 `required`) and balance stays $0.00 after reload. |
| MT-026 | Deposit | Medium | Deposit field accepts whole dollars only | Automated | `e2e/tests/deposit.spec.ts` › MT-026 Deposit field accepts whole dollars only | Browser marks 10.50 as `stepMismatch` (input has default step=1); no POST sent, balance unchanged. |
| MT-027 | Deposit | Medium | Large deposit is handled | Automated | `e2e/tests/deposit.spec.ts` › MT-027 Large deposit is handled |  |
| MT-028 | Deposit | Low | Letters cannot be typed into amount | Automated | `e2e/tests/deposit.spec.ts` › MT-028 Letters cannot be typed into amount | Types real keystrokes; Chromium drops letters in `type=number`. |
| MT-029 | Withdraw | High | Withdrawal decreases balance | Automated | `e2e/tests/withdraw.spec.ts` › MT-029 Withdrawal decreases balance |  |
| MT-030 | Withdraw | High | Customer can withdraw the full balance | Automated | `e2e/tests/withdraw.spec.ts` › MT-030 Customer can withdraw the full balance |  |
| MT-031 | Withdraw | High | Overdraw is rejected | Automated | `e2e/tests/withdraw.spec.ts` › MT-031 Overdraw is rejected |  |
| MT-032 | Withdraw | High | Withdrawal from empty account is rejected | Automated | `e2e/tests/withdraw.spec.ts` › MT-032 Withdrawal from empty account is rejected |  |
| MT-033 | Withdraw | High | Withdrawal appears in history | Automated | `e2e/tests/withdraw.spec.ts` › MT-033 Withdrawal appears in history |  |
| MT-034 | Withdraw | Medium | Empty withdrawal is blocked | Automated | `e2e/tests/withdraw.spec.ts` › MT-034 Empty withdrawal is blocked | Proves no POST /withdraw was sent. |
| MT-035 | Withdraw | High | Rejected withdrawal leaves no history | Automated | `e2e/tests/withdraw.spec.ts` › MT-035 Rejected withdrawal leaves no history |  |
| MT-036 | Transfer | High | Transfer debits the sender | Automated | `e2e/tests/transfer.spec.ts` › MT-036 Transfer debits the sender |  |
| MT-037 | Transfer | High | Transfer credits the recipient | Automated | `e2e/tests/transfer.spec.ts` › MT-037 Transfer credits the recipient | Recipient checked in a separate browser session. |
| MT-038 | Transfer | High | Sender history shows the transfer | Automated | `e2e/tests/transfer.spec.ts` › MT-038 Sender history shows the transfer |  |
| MT-039 | Transfer | High | Recipient history shows the transfer | Automated | `e2e/tests/transfer.spec.ts` › MT-039 Recipient history shows the transfer |  |
| MT-040 | Transfer | High | Customer can transfer the full balance | Automated | `e2e/tests/transfer.spec.ts` › MT-040 Customer can transfer the full balance |  |
| MT-041 | Transfer | High | Transfer above balance is rejected | Automated | `e2e/tests/transfer.spec.ts` › MT-041 Transfer above balance is rejected | Checks both sender ($100.00) and recipient ($0.00) balances. |
| MT-042 | Transfer | High | Transfer to unknown customer is rejected | Automated | `e2e/tests/transfer.spec.ts` › MT-042 Transfer to unknown customer is rejected | Precondition added: sender deposits 100 first. The app checks funds before the recipient, so a $0 sender would get "Insufficient funds" instead. |
| MT-043 | Transfer | Medium | Recipient is required | Automated | `e2e/tests/transfer.spec.ts` › MT-043 Recipient is required | Proves no POST /transfer was sent. |
| MT-044 | Transfer | Medium | Amount is required | Automated | `e2e/tests/transfer.spec.ts` › MT-044 Amount is required | Proves no POST /transfer was sent. |
| MT-045 | Transfer | High | Multiple transfers add up | Automated | `e2e/tests/transfer.spec.ts` › MT-045 Multiple transfers add up |  |
| MT-046 | Transfer | High | Rejected transfer leaves no history | Automated | `e2e/tests/transfer.spec.ts` › MT-046 Rejected transfer leaves no history |  |
| MT-047 | Transfer | High | Negative transfer amount | Not automated | — | Expected result "System handles it appropriately" is not defined, so there is nothing to assert without inventing a requirement. Observed today (MySQL 8, manual curl): with sender $100 and recipient $0, transferring -50 is accepted; sender becomes $150.00, recipient $-50.00, history shows "Transfer Out to <recipient>" -50.00. A deposit of -50 is also accepted and lowers the balance. The amount inputs have no `min`, so the browser does not block it. Needs a product decision (reject, and with what message) before a test can be written. |
| MT-048 | Transfer | Medium | Customer receives a transfer confirmation by SMS or email | Not automated | — | The app has no SMS or email capability (no mail/SMS dependency or code in pom.xml or src/), so there is nothing to observe. Also requires a real phone and inbox. Becomes automatable once a notification service exists (e.g. against a test mailbox). |
| MT-049 | Transactions | Medium | New customer has empty history | Automated | `e2e/tests/transactions.spec.ts` › MT-049 New customer has empty history |  |
| MT-050 | Transactions | Low | History columns are correct | Automated | `e2e/tests/transactions.spec.ts` › MT-050 History columns are correct |  |
| MT-051 | Transactions | High | All transaction types are listed | Automated | `e2e/tests/transactions.spec.ts` › MT-051 All transaction types are listed |  |
| MT-052 | Transactions | Low | Back to Dashboard link works | Automated | `e2e/tests/transactions.spec.ts` › MT-052 Back to Dashboard link works |  |
| MT-053 | Transactions | Low | Transactions link works from dashboard | Automated | `e2e/tests/transactions.spec.ts` › MT-053 Transactions link works from dashboard |  |
| MT-054 | Transactions | Medium | Every row has a date | Automated | `e2e/tests/transactions.spec.ts` › MT-054 Every row has a date | Asserts ISO date and time (e.g. 2026-10-23T13:48:14.679626) in every row. |
| MT-055 | Transactions | Low | Printed history is legible on the branch printer | Not automated | — | Requires a physical branch printer and a human reading the printout. A print-to-PDF check would not prove legibility on that printer. |
| MT-056 | Cross-cutting | High | Customers only see their own account | Automated | `e2e/tests/cross-cutting.spec.ts` › MT-056 Customers only see their own account | Two isolated browser contexts; each sees only its own name and balance. |
| MT-057 | Cross-cutting | High | Balance survives logout | Automated | `e2e/tests/cross-cutting.spec.ts` › MT-057 Balance survives logout |  |
| MT-058 | Cross-cutting | Low | Bank name shows on every page tab | Automated | `e2e/tests/cross-cutting.spec.ts` › MT-058 Bank name shows on every page tab | Asserts every tab title contains "Wells Fargo". |
| MT-059 | Cross-cutting | Medium | Screen reader reads balance and errors | Not automated | — | Requires JAWS on Windows and a human judging that announcements are "clear". An automated axe/ARIA scan could catch some issues but is not equivalent; can be added as a separate, smaller check if wanted. |
| MT-060 | Cross-cutting | Low | First-time customer completes a transfer unaided | Not automated | — | Moderated usability session with a real first-time customer; outcome depends on a human participant ("unaided", "under 2 minutes"). |
