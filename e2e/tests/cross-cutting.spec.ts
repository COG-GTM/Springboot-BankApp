import { expect, test } from '@playwright/test';
import { deposit, expectBalance, loginToDashboard, newCustomer, registerViaHttp, sessionFor, uniqueUsername } from './helpers';

const BANK_NAME = 'Wells Fargo';

test.describe('Cross-cutting', () => {
  test('MT-056 Customers only see their own account', async ({ page, request, browser }) => {
    const alice = await newCustomer(page, request, 'mt056_a');
    await deposit(page, '100');
    const bob = uniqueUsername('mt056_b');
    await registerViaHttp(request, bob);
    const bobPage = await sessionFor(browser, bob);
    await deposit(bobPage, '250');

    await page.reload();
    await expect(page.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${alice}`);
    await expectBalance(page, '100.00');
    await expect(page.getByText(bob)).toHaveCount(0);

    await expect(bobPage.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${bob}`);
    await expectBalance(bobPage, '250.00');
    await expect(bobPage.getByText(alice)).toHaveCount(0);
    await bobPage.context().close();
  });

  test('MT-057 Balance survives logout', async ({ page, request }) => {
    const username = await newCustomer(page, request, 'mt057');
    await deposit(page, '100');
    await expectBalance(page, '100.00');
    await page.getByRole('link', { name: 'Logout' }).click();
    await expect(page).toHaveURL(/\/login\?logout$/);
    await loginToDashboard(page, username);
    await expectBalance(page, '100.00');
  });

  test('MT-058 Bank name shows on every page tab', async ({ page, request }) => {
    await page.goto('/login');
    await expect(page).toHaveTitle(new RegExp(BANK_NAME));
    await page.goto('/register');
    await expect(page).toHaveTitle(new RegExp(BANK_NAME));
    await newCustomer(page, request, 'mt058');
    await expect(page).toHaveTitle(new RegExp(BANK_NAME));
    await page.goto('/transactions');
    await expect(page).toHaveTitle(new RegExp(BANK_NAME));
  });
});
