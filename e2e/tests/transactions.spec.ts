import { expect, test } from '@playwright/test';
import {
  deposit,
  historyRows,
  historyTypesAndAmounts,
  newCustomer,
  openTransactions,
  registerViaHttp,
  transfer,
  uniqueUsername,
  withdraw,
} from './helpers';

test.describe('Transactions', () => {
  test('MT-049 New customer has empty history', async ({ page, request }) => {
    await newCustomer(page, request, 'mt049');
    await openTransactions(page);
    await expect(page.locator('table thead th')).toHaveCount(4);
    await expect(historyRows(page)).toHaveCount(0);
  });

  test('MT-050 History columns are correct', async ({ page, request }) => {
    await newCustomer(page, request, 'mt050');
    await openTransactions(page);
    await expect(page.locator('table thead th')).toHaveText(['ID', 'Type', 'Amount', 'Date']);
  });

  test('MT-051 All transaction types are listed', async ({ page, request }) => {
    const recipient = uniqueUsername('mt051_to');
    await registerViaHttp(request, recipient);
    await newCustomer(page, request, 'mt051');
    await deposit(page, '100');
    await withdraw(page, '10');
    await transfer(page, recipient, '20');
    await openTransactions(page);
    const rows = await historyTypesAndAmounts(page);
    expect(rows).toContainEqual({ type: 'Deposit', amount: '100.00' });
    expect(rows).toContainEqual({ type: 'Withdrawal', amount: '10.00' });
    expect(rows).toContainEqual({ type: `Transfer Out to ${recipient}`, amount: '20.00' });
  });

  test('MT-052 Back to Dashboard link works', async ({ page, request }) => {
    const username = await newCustomer(page, request, 'mt052');
    await openTransactions(page);
    await page.getByRole('link', { name: 'Back to Dashboard' }).click();
    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${username}`);
  });

  test('MT-053 Transactions link works from dashboard', async ({ page, request }) => {
    await newCustomer(page, request, 'mt053');
    await page.locator('nav').getByRole('link', { name: 'Transactions', exact: true }).click();
    await expect(page).toHaveURL(/\/transactions$/);
    await expect(page.getByRole('heading', { name: 'Transaction History' })).toBeVisible();
  });

  test('MT-054 Every row has a date', async ({ page, request }) => {
    await newCustomer(page, request, 'mt054');
    await deposit(page, '100');
    await withdraw(page, '10');
    await openTransactions(page);
    const rows = historyRows(page);
    await expect(rows).toHaveCount(2);
    for (let i = 0; i < 2; i++) {
      await expect(rows.nth(i).locator('td').nth(3)).toHaveText(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?$/);
    }
  });
});
