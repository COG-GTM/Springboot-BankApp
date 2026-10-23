import { expect, test } from '@playwright/test';
import { expectBalance, newCustomer, openPanel } from './helpers';

test.describe('Dashboard', () => {
  test('MT-014 New customer starts at zero', async ({ page, request }) => {
    await newCustomer(page, request, 'mt014');
    await expectBalance(page, '0.00');
  });

  test('MT-015 Welcome message shows the customer', async ({ page, request }) => {
    const username = await newCustomer(page, request, 'mt015');
    await expect(page.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${username}`);
  });

  test('MT-016 Account number is shown', async ({ page, request }) => {
    await newCustomer(page, request, 'mt016');
    const details = page.locator('.account-details');
    await expect(details.getByRole('heading', { name: 'Account Details' })).toBeVisible();
    const accountNumber = details.locator('p', { hasText: 'Account Number:' }).locator('span');
    await expect(accountNumber).toHaveText(/^\d+$/);
  });

  test('MT-017 Navigation links are present', async ({ page, request }) => {
    await newCustomer(page, request, 'mt017');
    const nav = page.locator('nav');
    for (const name of ['Dashboard', 'Transactions', 'Logout']) {
      await expect(nav.getByRole('link', { name, exact: true })).toBeVisible();
    }
  });

  test('MT-018 Deposit panel opens', async ({ page, request }) => {
    await newCustomer(page, request, 'mt018');
    await expect(page.locator('#depositForm input[name="amount"]')).toBeHidden();
    const form = await openPanel(page, 'deposit');
    await expect(form.locator('input[name="amount"]')).toBeVisible();
  });

  test('MT-019 Withdraw panel opens', async ({ page, request }) => {
    await newCustomer(page, request, 'mt019');
    await expect(page.locator('#withdrawForm input[name="amount"]')).toBeHidden();
    const form = await openPanel(page, 'withdraw');
    await expect(form.locator('input[name="amount"]')).toBeVisible();
  });

  test('MT-020 Transfer panel opens', async ({ page, request }) => {
    await newCustomer(page, request, 'mt020');
    await expect(page.locator('#transferForm input[name="toUsername"]')).toBeHidden();
    const form = await openPanel(page, 'transfer');
    await expect(form.locator('input[name="toUsername"]')).toBeVisible();
    await expect(form.locator('input[name="amount"]')).toBeVisible();
  });
});
