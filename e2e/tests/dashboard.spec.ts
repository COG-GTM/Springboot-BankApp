import { test, expect } from '@playwright/test';
import { expectBalance, newCustomer, openPanel, panel } from './helpers';

test.describe('Dashboard', () => {
  test('MT-014 New customer starts at zero', async ({ page }) => {
    await newCustomer(page, 'zero');
    await expectBalance(page, '0.00');
  });

  test('MT-015 Welcome message shows the customer', async ({ page }) => {
    const username = await newCustomer(page, 'welcome');
    await expect(page.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${username}`);
  });

  test('MT-016 Account number is shown', async ({ page }) => {
    await newCustomer(page, 'acctno');
    const details = page.locator('.account-details');
    await expect(details.getByRole('heading', { name: 'Account Details' })).toBeVisible();
    const accountNumber = details.locator('p', { hasText: 'Account Number:' }).locator('span');
    await expect(accountNumber).toHaveText(/^\d+$/);
  });

  test('MT-017 Navigation links are present', async ({ page }) => {
    await newCustomer(page, 'nav');
    const nav = page.locator('nav');
    for (const name of ['Dashboard', 'Transactions', 'Logout']) {
      await expect(nav.getByRole('link', { name, exact: true })).toBeVisible();
    }
  });

  test('MT-018 Deposit panel opens', async ({ page }) => {
    await newCustomer(page, 'pdep');
    await expect(panel(page, 'depositForm')).toBeHidden();
    await openPanel(page, 'depositForm');
    await expect(panel(page, 'depositForm').locator('input[name="amount"]')).toBeVisible();
  });

  test('MT-019 Withdraw panel opens', async ({ page }) => {
    await newCustomer(page, 'pwd');
    await expect(panel(page, 'withdrawForm')).toBeHidden();
    await openPanel(page, 'withdrawForm');
    await expect(panel(page, 'withdrawForm').locator('input[name="amount"]')).toBeVisible();
  });

  test('MT-020 Transfer panel opens', async ({ page }) => {
    await newCustomer(page, 'ptr');
    await expect(panel(page, 'transferForm')).toBeHidden();
    await page.getByRole('button', { name: /Transfer/ }).click();
    await expect(panel(page, 'transferForm').locator('input[name="toUsername"]')).toBeVisible();
    await expect(panel(page, 'transferForm').locator('input[name="amount"]')).toBeVisible();
  });
});
