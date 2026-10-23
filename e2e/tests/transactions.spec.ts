import { test, expect, deposit, history, newCustomer, registerViaApi, transfer, uniqueUsername, withdraw } from './fixtures';

const DATE_TIME = /^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}(:\d{2}(\.\d+)?)?$/;

test.describe('Transactions', () => {
  test('MT-049 New customer has empty history', async ({ page }) => {
    await newCustomer(page);
    await page.goto('/transactions');
    await expect(page.locator('table thead th')).toHaveCount(4);
    await expect(page.locator('table tbody tr')).toHaveCount(0);
  });

  test('MT-050 History columns are correct', async ({ page }) => {
    await newCustomer(page);
    await page.goto('/transactions');
    await expect(page.locator('table thead th')).toHaveText(['ID', 'Type', 'Amount', 'Date']);
  });

  test('MT-051 All transaction types are listed', async ({ page }) => {
    const payee = uniqueUsername('payee');
    await registerViaApi(page, payee);
    await newCustomer(page);
    await deposit(page, '100');
    await withdraw(page, '10');
    await transfer(page, payee, '20');
    const types = (await history(page)).map((r) => r.type);
    expect(types).toEqual(expect.arrayContaining(['Deposit', 'Withdrawal', `Transfer Out to ${payee}`]));
    expect(types).toHaveLength(3);
  });

  test('MT-052 Back to Dashboard link works', async ({ page }) => {
    await newCustomer(page);
    await page.goto('/transactions');
    await page.getByRole('link', { name: 'Back to Dashboard' }).click();
    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole('heading', { name: /^Welcome, / })).toBeVisible();
  });

  test('MT-053 Transactions link works from dashboard', async ({ page }) => {
    await newCustomer(page);
    await page.locator('nav').getByRole('link', { name: 'Transactions', exact: true }).click();
    await expect(page).toHaveURL(/\/transactions$/);
    await expect(page.getByRole('heading', { name: 'Transaction History' })).toBeVisible();
  });

  test('MT-054 Every row has a date', async ({ page }) => {
    await newCustomer(page);
    await deposit(page, '100');
    await withdraw(page, '10');
    const rows = await history(page);
    expect(rows).toHaveLength(2);
    for (const row of rows) expect(row.date).toMatch(DATE_TIME);
  });
});
