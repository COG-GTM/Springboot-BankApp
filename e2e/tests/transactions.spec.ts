import { test, expect } from '@playwright/test';
import { deposit, expectBalance, historyEntries, historyRows, newCustomer, registerOther, transfer, withdraw } from './helpers';

test.describe('Transactions', () => {
  test('MT-049 New customer has empty history', async ({ page }) => {
    await newCustomer(page, 'hist0');
    await page.goto('/transactions');
    await expect(page.locator('table thead th')).toHaveCount(4);
    await expect(historyRows(page)).toHaveCount(0);
  });

  test('MT-050 History columns are correct', async ({ page }) => {
    await newCustomer(page, 'cols');
    await page.goto('/transactions');
    await expect(page.locator('table thead th')).toHaveText(['ID', 'Type', 'Amount', 'Date']);
  });

  test('MT-051 All transaction types are listed', async ({ page, browser }) => {
    await newCustomer(page, 'alltypes');
    const recipient = await registerOther(browser);
    await deposit(page, '100');
    await withdraw(page, '10');
    await transfer(page, recipient, '20');
    await expectBalance(page, '70.00');
    const types = (await historyEntries(page)).map((r) => r.type);
    expect(types).toContain('Deposit');
    expect(types).toContain('Withdrawal');
    expect(types).toContain(`Transfer Out to ${recipient}`);
  });

  test('MT-052 Back to Dashboard link works', async ({ page }) => {
    await newCustomer(page, 'back');
    await page.goto('/transactions');
    await page.getByRole('link', { name: 'Back to Dashboard' }).click();
    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole('heading', { name: /^Welcome, / })).toBeVisible();
  });

  test('MT-053 Transactions link works from dashboard', async ({ page }) => {
    await newCustomer(page, 'txlink');
    await page.locator('nav').getByRole('link', { name: 'Transactions', exact: true }).click();
    await expect(page).toHaveURL(/\/transactions$/);
    await expect(page.getByRole('heading', { name: 'Transaction History' })).toBeVisible();
  });

  test('MT-054 Every row has a date', async ({ page }) => {
    await newCustomer(page, 'dates');
    await deposit(page, '100');
    await deposit(page, '50');
    await expectBalance(page, '150.00');
    const rows = await historyEntries(page);
    expect(rows).toHaveLength(2);
    for (const row of rows) {
      expect(row.date).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?$/);
    }
  });
});
