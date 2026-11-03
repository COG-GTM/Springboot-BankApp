import { test, expect, PASSWORD, uniqueUser } from './fixtures';

test.describe('Authentication', () => {
  test('MT-001 login page shows username and password fields', async ({ app, page }) => {
    await app.gotoLogin();
    await expect(page.locator('input[name="username"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Login' })).toBeVisible();
  });

  test('MT-002 register link on login page opens registration', async ({ app, page }) => {
    await app.gotoLogin();
    await page.getByRole('link', { name: 'Register here' }).click();
    await expect(page.getByRole('heading', { name: 'Register a New Account' })).toBeVisible();
  });

  test('MT-003 login link on register page returns to login', async ({ page }) => {
    await page.goto('/register');
    await page.getByRole('link', { name: 'Login here' }).click();
    await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
  });

  test('MT-004 new customer can register', async ({ app, page }) => {
    await app.register(uniqueUser());
    await expect(page).toHaveURL(/\/login$/);
  });

  test('MT-005 duplicate username is rejected', async ({ app, page, customer }) => {
    await app.register(customer);
    await expect(page.locator('.alert-danger')).toContainText('User already present.');
  });

  test('MT-006 valid login opens dashboard', async ({ app, page, customer }) => {
    await app.login(customer);
    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole('heading', { name: `Welcome, ${customer}` })).toBeVisible();
  });

  test('MT-007 wrong password is rejected', async ({ app, page, customer }) => {
    await app.login(customer, 'not-the-password');
    await expect(page).toHaveURL(/\/login\?error/);
    await expect(page.locator('.alert-danger')).toContainText('Invalid username or password.');
  });

  test('MT-008 unknown username gets the same error', async ({ app, page }) => {
    await app.login(uniqueUser('ghost'));
    await expect(page.locator('.alert-danger')).toContainText('Invalid username or password.');
  });

  test('MT-009 password is case sensitive', async ({ app, page, customer }) => {
    await app.login(customer, PASSWORD.toUpperCase());
    await expect(page).toHaveURL(/\/login\?error/);
  });

  test('MT-010 dashboard requires login', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('MT-011 transaction history requires login', async ({ page }) => {
    await page.goto('/transactions');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('MT-012 logout returns to login', async ({ loggedIn, page }) => {
    await loggedIn.logout();
    await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
  });

  test('MT-013 session ends after logout', async ({ loggedIn, page }) => {
    await loggedIn.logout();
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);
  });
});
