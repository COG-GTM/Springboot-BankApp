import { test, expect } from './fixtures';

test.describe('Dashboard', () => {
  test('MT-014 new customer starts at zero', async ({ loggedIn }) => {
    await loggedIn.expectBalance('0.00');
  });

  test('MT-015 welcome message shows the customer', async ({ loggedIn, page, customer }) => {
    await expect(page.getByRole('heading', { name: `Welcome, ${customer}` })).toBeVisible();
  });

  test('MT-016 account number is shown', async ({ loggedIn, page }) => {
    const accountNumber = page.locator('p', { hasText: 'Account Number:' }).locator('span');
    await expect(accountNumber).toHaveText(/^\d+$/);
  });

  test('MT-017 navigation links are present', async ({ loggedIn, page }) => {
    for (const name of ['Dashboard', 'Transactions', 'Logout']) {
      await expect(page.getByRole('link', { name, exact: true })).toBeVisible();
    }
  });

  test('MT-018 deposit panel opens', async ({ loggedIn }) => {
    await loggedIn.openPanel('deposit');
    await expect(loggedIn.form('deposit').locator('input[name="amount"]')).toBeVisible();
  });

  test('MT-019 withdraw panel opens', async ({ loggedIn }) => {
    await loggedIn.openPanel('withdraw');
    await expect(loggedIn.form('withdraw').locator('input[name="amount"]')).toBeVisible();
  });

  test('MT-020 transfer panel opens', async ({ loggedIn }) => {
    await loggedIn.openPanel('transfer');
    await expect(loggedIn.form('transfer').locator('input[name="toUsername"]')).toBeVisible();
    await expect(loggedIn.form('transfer').locator('input[name="amount"]')).toBeVisible();
  });
});
