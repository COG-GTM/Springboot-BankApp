import { test, expect } from '@playwright/test';
import {
  deposit, historyRow, historyRows, newCustomerSession, openHistory, registerCustomer, transfer, withdraw,
} from './helpers';

const COLUMNS = ['ID', 'Type', 'Amount', 'Date'];

test.describe('Transactions', () => {
  test('MT-049 New customer has empty history', async ({ page }) => {
    await newCustomerSession(page, 'mt049');
    await openHistory(page);
    await expect(page.locator('table thead th')).toHaveText(COLUMNS);
    await expect(historyRows(page)).toHaveCount(0);
  });

  test('MT-050 History columns are correct', async ({ page }) => {
    await newCustomerSession(page, 'mt050');
    await openHistory(page);
    await expect(page.locator('table thead th')).toHaveText(COLUMNS);
  });

  test('MT-051 All transaction types are listed', async ({ page }) => {
    await newCustomerSession(page, 'mt051');
    const recipient = await registerCustomer(page.request, 'mt051r');
    await deposit(page, 100);
    await withdraw(page, 10);
    await transfer(page, recipient.username, 20);
    await openHistory(page);
    await expect(historyRow(page, 'Deposit')).toHaveCount(1);
    await expect(historyRow(page, 'Withdrawal')).toHaveCount(1);
    await expect(historyRow(page, `Transfer Out to ${recipient.username}`)).toHaveCount(1);
  });

  test('MT-052 Back to Dashboard link works', async ({ page }) => {
    const customer = await newCustomerSession(page, 'mt052');
    await openHistory(page);
    await page.getByRole('link', { name: 'Back to Dashboard' }).click();
    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole('heading', { name: `Welcome, ${customer.username}` })).toBeVisible();
  });

  test('MT-053 Transactions link works from dashboard', async ({ page }) => {
    await newCustomerSession(page, 'mt053');
    await page.locator('nav').getByRole('link', { name: 'Transactions', exact: true }).click();
    await expect(page).toHaveURL(/\/transactions$/);
    await expect(page.getByRole('heading', { name: 'Transaction History' })).toBeVisible();
  });

  test('MT-054 Every row has a date', async ({ page }) => {
    await newCustomerSession(page, 'mt054');
    await deposit(page, 100);
    await deposit(page, 5);
    await openHistory(page);
    const rows = historyRows(page);
    await expect(rows).toHaveCount(2);
    for (const row of await rows.all()) {
      await expect(row.locator('td').nth(3)).toHaveText(/^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}/);
    }
  });
});
