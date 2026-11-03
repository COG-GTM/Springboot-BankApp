import { test, expect, BankApp, registerViaApi, uniqueUser } from './fixtures';

test.describe('Cross-cutting', () => {
  test('MT-056 customers only see their own account', async ({ loggedIn, page, customer, browser, request }) => {
    const other = uniqueUser('other');
    await registerViaApi(request, other);
    await loggedIn.deposit('100');

    const otherContext = await browser.newContext();
    const otherApp = new BankApp(await otherContext.newPage());
    await otherApp.login(other);
    await expect(otherApp.page.getByRole('heading', { name: `Welcome, ${other}` })).toBeVisible();
    await otherApp.expectBalance('0.00');

    await page.reload();
    await expect(page.getByRole('heading', { name: `Welcome, ${customer}` })).toBeVisible();
    await loggedIn.expectBalance('100.00');
    await otherContext.close();
  });

  test('MT-057 balance survives logout', async ({ loggedIn, customer }) => {
    await loggedIn.deposit('100');
    await loggedIn.expectBalance('100.00');
    await loggedIn.logout();
    await loggedIn.login(customer);
    await loggedIn.expectBalance('100.00');
  });

  test('MT-058 bank name shows on every page tab', async ({ app, page, customer }) => {
    await page.goto('/register');
    await expect(page).toHaveTitle(/Wells Fargo/);
    await app.login(customer);
    await expect(page).toHaveTitle(/Wells Fargo/);
    await app.openTransactions();
    await expect(page).toHaveTitle(/Wells Fargo/);
    await app.logout();
    await expect(page).toHaveTitle(/Wells Fargo/);
  });
});
