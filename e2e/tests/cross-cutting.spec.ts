import { test, expect } from '@playwright/test';
import { deposit, expectBalance, login, newCustomerSession, registerCustomer } from './helpers';

const BANK_NAME = 'Wells Fargo';

test.describe('Cross-cutting', () => {
  test('MT-056 Customers only see their own account', async ({ browser }) => {
    const contextA = await browser.newContext();
    const contextB = await browser.newContext();
    const pageA = await contextA.newPage();
    const pageB = await contextB.newPage();
    const [a, b] = await Promise.all([newCustomerSession(pageA, 'mt056a'), newCustomerSession(pageB, 'mt056b')]);
    await deposit(pageA, 100);
    await deposit(pageB, 25);
    await Promise.all([pageA.reload(), pageB.reload()]);

    await expect(pageA.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${a.username}`);
    await expectBalance(pageA, '100.00');
    await expect(pageA.locator('body')).not.toContainText(b.username);

    await expect(pageB.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${b.username}`);
    await expectBalance(pageB, '25.00');
    await expect(pageB.locator('body')).not.toContainText(a.username);

    await contextA.close();
    await contextB.close();
  });

  test('MT-057 Balance survives logout', async ({ page }) => {
    const customer = await newCustomerSession(page, 'mt057');
    await deposit(page, 100);
    await page.getByRole('link', { name: 'Logout' }).click();
    await expect(page).toHaveURL(/\/login\?logout$/);
    await login(page, customer);
    await expectBalance(page, '100.00');
  });

  test('MT-058 Bank name shows on every page tab', async ({ page }) => {
    const customer = await registerCustomer(page.request, 'mt058');
    await page.goto('/login');
    await expect(page).toHaveTitle(new RegExp(BANK_NAME));
    await page.goto('/register');
    await expect(page).toHaveTitle(new RegExp(BANK_NAME));
    await login(page, customer);
    await expect(page).toHaveTitle(new RegExp(BANK_NAME));
    await page.goto('/transactions');
    await expect(page).toHaveTitle(new RegExp(BANK_NAME));
  });
});
