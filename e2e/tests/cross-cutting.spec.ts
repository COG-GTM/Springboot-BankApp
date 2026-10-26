import { test, expect } from '@playwright/test';
import { deposit, expectBalance, login, newCustomer } from './helpers';

test.describe('Cross-cutting', () => {
  test('MT-056 Customers only see their own account', async ({ browser }) => {
    const ctxA = await browser.newContext();
    const ctxB = await browser.newContext();
    const pageA = await ctxA.newPage();
    const pageB = await ctxB.newPage();
    const userA = await newCustomer(pageA, 'alice');
    const userB = await newCustomer(pageB, 'bob');
    await deposit(pageA, '100');
    await deposit(pageB, '25');

    await pageA.reload();
    await pageB.reload();
    await expect(pageA.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${userA}`);
    await expectBalance(pageA, '100.00');
    await expect(pageB.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${userB}`);
    await expectBalance(pageB, '25.00');
    await expect(pageA.getByText(userB)).toHaveCount(0);
    await expect(pageB.getByText(userA)).toHaveCount(0);
    await ctxA.close();
    await ctxB.close();
  });

  test('MT-057 Balance survives logout', async ({ page }) => {
    const username = await newCustomer(page, 'persist');
    await deposit(page, '100');
    await expectBalance(page, '100.00');
    await page.getByRole('link', { name: 'Logout' }).click();
    await expect(page).toHaveURL(/\/login\?logout$/);
    await login(page, username);
    await expect(page).toHaveURL(/\/dashboard$/);
    await expectBalance(page, '100.00');
  });

  // The plan does not name the bank, so the expected name is read from the brand in the page header.
  test('MT-058 Bank name shows on every page tab', async ({ page }) => {
    await page.goto('/login');
    const bankName = (await page.locator('.navbar-brand').innerText()).trim();
    expect(bankName).not.toBe('');

    const titles: Record<string, string> = {};
    titles.login = await page.title();
    await page.goto('/register');
    titles.register = await page.title();
    await newCustomer(page, 'title');
    titles.dashboard = await page.title();
    await page.goto('/transactions');
    titles.transactions = await page.title();

    for (const [name, title] of Object.entries(titles)) {
      expect(title, `${name} tab title`).toContain(bankName);
    }
  });
});
