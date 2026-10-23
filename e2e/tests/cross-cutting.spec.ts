import { test, expect, deposit, expectBalance, login, logout, newCustomer, registerViaApi, sessionFor, uniqueUsername } from './fixtures';

const BANK_NAME = 'Wells Fargo';

test.describe('Cross-cutting', () => {
  test('MT-056 Customers only see their own account', async ({ page, browser, baseURL }) => {
    const alice = await newCustomer(page, 'alice');
    await deposit(page, '100');
    const bob = uniqueUsername('bob');
    await registerViaApi(page, bob);
    const bobPage = await sessionFor(browser, baseURL, bob);
    await deposit(bobPage, '25');

    await page.goto('/dashboard');
    await expect(page.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${alice}`);
    await expectBalance(page, '100.00');
    await expect(page.getByText(bob)).toHaveCount(0);

    await bobPage.goto('/dashboard');
    await expect(bobPage.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${bob}`);
    await expectBalance(bobPage, '25.00');
    await expect(bobPage.getByText(alice)).toHaveCount(0);
  });

  test('MT-057 Balance survives logout', async ({ page }) => {
    const username = await newCustomer(page);
    await deposit(page, '100');
    await logout(page);
    await login(page, username);
    await expect(page).toHaveURL(/\/dashboard$/);
    await expectBalance(page, '100.00');
  });

  test('MT-058 Bank name shows on every page tab', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveTitle(new RegExp(BANK_NAME));
    await page.goto('/register');
    await expect(page).toHaveTitle(new RegExp(BANK_NAME));
    await newCustomer(page);
    await expect(page).toHaveTitle(new RegExp(BANK_NAME));
    await page.goto('/transactions');
    await expect(page).toHaveTitle(new RegExp(BANK_NAME));
  });
});
