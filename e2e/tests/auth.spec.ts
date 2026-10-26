import { test, expect } from '@playwright/test';
import { PASSWORD, login, newCustomer, register, uniqueUsername } from './helpers';

const INVALID_LOGIN = 'Invalid username or password.';

test.describe('Authentication', () => {
  test('MT-001 Login page shows username and password fields', async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('input[name="username"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Login' })).toBeVisible();
  });

  test('MT-002 Register link on login page opens registration', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('link', { name: 'Register here' }).click();
    await expect(page).toHaveURL(/\/register$/);
    await expect(page.getByRole('heading', { name: 'Register a New Account' })).toBeVisible();
  });

  test('MT-003 Login link on register page returns to login', async ({ page }) => {
    await page.goto('/register');
    await page.getByRole('link', { name: 'Login here' }).click();
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
  });

  test('MT-004 New customer can register', async ({ page }) => {
    await register(page, uniqueUsername('reg'));
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
  });

  test('MT-005 Duplicate username is rejected', async ({ page }) => {
    const username = uniqueUsername('dup');
    await register(page, username);
    await expect(page).toHaveURL(/\/login$/);
    await register(page, username);
    await expect(page.getByText('User already present.')).toBeVisible();
  });

  test('MT-006 Valid login opens dashboard', async ({ page }) => {
    const username = await newCustomer(page, 'login');
    await expect(page.getByRole('heading', { name: `Welcome, ${username}` })).toBeVisible();
  });

  test('MT-007 Wrong password is rejected', async ({ page }) => {
    const username = uniqueUsername('wrongpw');
    await register(page, username);
    await login(page, username, 'Not-the-password1');
    await expect(page).toHaveURL(/\/login\?error/);
    await expect(page.getByText(INVALID_LOGIN)).toBeVisible();
  });

  test('MT-008 Unknown username gets the same error', async ({ page }) => {
    const known = uniqueUsername('known');
    await register(page, known);
    await login(page, known, 'Not-the-password1');
    const wrongPasswordAlert = (await page.locator('.alert-danger').innerText()).trim();

    await login(page, uniqueUsername('ghost'), PASSWORD);
    await expect(page).toHaveURL(/\/login\?error/);
    await expect(page.locator('.alert-danger')).toHaveText(wrongPasswordAlert);
    await expect(page.locator('.alert-danger')).toHaveText(INVALID_LOGIN);
  });

  test('MT-009 Password is case sensitive', async ({ page }) => {
    const username = uniqueUsername('case');
    await register(page, username);
    const flipped = PASSWORD.replace(/[a-z]/gi, (c) => (c === c.toLowerCase() ? c.toUpperCase() : c.toLowerCase()));
    expect(flipped).not.toBe(PASSWORD);
    await login(page, username, flipped);
    await expect(page).toHaveURL(/\/login\?error/);
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('MT-010 Dashboard requires login', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('MT-011 Transaction history requires login', async ({ page }) => {
    await page.goto('/transactions');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('MT-012 Logout returns to login', async ({ page }) => {
    await newCustomer(page, 'logout');
    await page.getByRole('link', { name: 'Logout' }).click();
    await expect(page).toHaveURL(/\/login\?logout$/);
    await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
  });

  test('MT-013 Session ends after logout', async ({ page }) => {
    await newCustomer(page, 'session');
    await page.getByRole('link', { name: 'Logout' }).click();
    await expect(page).toHaveURL(/\/login\?logout$/);
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);
  });
});
