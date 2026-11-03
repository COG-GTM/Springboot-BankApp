import { test, expect, registerViaApi, uniqueUser } from './fixtures';

test.describe('Transactions', () => {
  test('MT-049 new customer has empty history', async ({ loggedIn, page }) => {
    await loggedIn.openTransactions();
    await expect(page.locator('table thead')).toBeVisible();
    await expect(loggedIn.historyRows()).toHaveCount(0);
  });

  test('MT-050 history columns are correct', async ({ loggedIn, page }) => {
    await loggedIn.openTransactions();
    await expect(page.locator('table thead th')).toHaveText(['ID', 'Type', 'Amount', 'Date']);
  });

  test('MT-051 all transaction types are listed', async ({ loggedIn, request }) => {
    const payee = uniqueUser('payee');
    await registerViaApi(request, payee);
    await loggedIn.deposit('100');
    await loggedIn.withdraw('10');
    await loggedIn.transfer(payee, '20');
    await loggedIn.expectBalance('70.00');
    await loggedIn.openTransactions();
    await expect(loggedIn.historyRow('Deposit')).toHaveCount(1);
    await expect(loggedIn.historyRow('Withdrawal')).toHaveCount(1);
    await expect(loggedIn.historyRow(`Transfer Out to ${payee}`)).toHaveCount(1);
  });

  test('MT-052 back to dashboard link works', async ({ loggedIn, page }) => {
    await loggedIn.openTransactions();
    await page.getByRole('link', { name: 'Back to Dashboard' }).click();
    await expect(page).toHaveURL(/\/dashboard$/);
    await loggedIn.expectBalance('0.00');
  });

  test('MT-053 transactions link works from dashboard', async ({ loggedIn, page }) => {
    await page.getByRole('link', { name: 'Transactions', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Transaction History' })).toBeVisible();
  });

  test('MT-054 every row has a date', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.withdraw('10');
    await loggedIn.expectBalance('90.00');
    await loggedIn.openTransactions();
    const rows = loggedIn.historyRows();
    await expect(rows).toHaveCount(2);
    for (const row of await rows.all()) {
      await expect(row.locator('td').nth(3)).toHaveText(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/);
    }
  });
});
