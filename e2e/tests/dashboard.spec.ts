import { test, expect } from '@playwright/test';
import { expectBalance, newCustomerSession, openPanel } from './helpers';

test.describe('Dashboard', () => {
  test('MT-014 New customer starts at zero', async ({ page }) => {
    await newCustomerSession(page, 'mt014');
    await expectBalance(page, '0.00');
  });

  test('MT-015 Welcome message shows the customer', async ({ page }) => {
    const customer = await newCustomerSession(page, 'mt015');
    await expect(page.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${customer.username}`);
  });

  test('MT-016 Account number is shown', async ({ page }) => {
    await newCustomerSession(page, 'mt016');
    const details = page.locator('.account-details');
    await expect(details.getByRole('heading', { name: 'Account Details' })).toBeVisible();
    await expect(details.locator('p', { hasText: 'Account Number:' }).locator('span')).toHaveText(/^\d+$/);
  });

  test('MT-017 Navigation links are present', async ({ page }) => {
    await newCustomerSession(page, 'mt017');
    const nav = page.locator('nav');
    for (const name of ['Dashboard', 'Transactions', 'Logout']) {
      await expect(nav.getByRole('link', { name, exact: true })).toBeVisible();
    }
  });

  test('MT-018 Deposit panel opens', async ({ page }) => {
    await newCustomerSession(page, 'mt018');
    await expect(page.locator('#depositForm input[name="amount"]')).toBeHidden();
    const form = await openPanel(page, 'Deposit');
    await expect(form.locator('input[name="amount"]')).toBeVisible();
  });

  test('MT-019 Withdraw panel opens', async ({ page }) => {
    await newCustomerSession(page, 'mt019');
    await expect(page.locator('#withdrawForm input[name="amount"]')).toBeHidden();
    const form = await openPanel(page, 'Withdraw');
    await expect(form.locator('input[name="amount"]')).toBeVisible();
  });

  test('MT-020 Transfer panel opens', async ({ page }) => {
    await newCustomerSession(page, 'mt020');
    await expect(page.locator('#transferForm input[name="toUsername"]')).toBeHidden();
    const form = await openPanel(page, 'Transfer Money');
    await expect(form.locator('input[name="toUsername"]')).toBeVisible();
    await expect(form.locator('input[name="amount"]')).toBeVisible();
  });
});
