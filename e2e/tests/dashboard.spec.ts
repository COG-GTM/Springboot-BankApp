import { test, expect, expectBalance, newCustomer, openPanel } from './fixtures';

test.describe('Dashboard', () => {
  test('MT-014 New customer starts at zero', async ({ page }) => {
    await newCustomer(page);
    await expectBalance(page, '0.00');
  });

  test('MT-015 Welcome message shows the customer', async ({ page }) => {
    const username = await newCustomer(page);
    await expect(page.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${username}`);
  });

  test('MT-016 Account number is shown', async ({ page }) => {
    await newCustomer(page);
    await expect(page.getByRole('heading', { name: 'Account Details' })).toBeVisible();
    const number = page.locator('p', { hasText: 'Account Number:' }).locator('span');
    await expect(number).toHaveText(/^\d+$/);
  });

  test('MT-017 Navigation links are present', async ({ page }) => {
    await newCustomer(page);
    const nav = page.locator('nav');
    for (const name of ['Dashboard', 'Transactions', 'Logout']) {
      await expect(nav.getByRole('link', { name, exact: true })).toBeVisible();
    }
  });

  test('MT-018 Deposit panel opens', async ({ page }) => {
    await newCustomer(page);
    const form = await openPanel(page, 'deposit');
    await expect(form.locator('input[name="amount"]')).toBeVisible();
  });

  test('MT-019 Withdraw panel opens', async ({ page }) => {
    await newCustomer(page);
    const form = await openPanel(page, 'withdraw');
    await expect(form.locator('input[name="amount"]')).toBeVisible();
  });

  test('MT-020 Transfer panel opens', async ({ page }) => {
    await newCustomer(page);
    const form = await openPanel(page, 'transfer');
    await expect(form.locator('input[name="toUsername"]')).toBeVisible();
    await expect(form.locator('input[name="amount"]')).toBeVisible();
  });
});
