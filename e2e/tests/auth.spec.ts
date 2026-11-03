import { test, expect } from '@playwright/test';
import { PASSWORD, login, registerCustomer, uniqueName } from './helpers';

const INVALID_LOGIN = 'Invalid username or password.';

async function attemptLogin(page: import('@playwright/test').Page, username: string, password: string) {
  await page.goto('/login');
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[name="password"]').fill(password);
  await page.getByRole('button', { name: 'Login' }).click();
}

test.describe('Authentication', () => {
  test('MT-001 Login page shows username and password fields', async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('input[name="username"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toHaveAttribute('type', 'password');
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
    await page.goto('/register');
    await page.locator('input[name="username"]').fill(uniqueName('mt004'));
    await page.locator('input[name="password"]').fill(PASSWORD);
    await page.getByRole('button', { name: 'Register' }).click();
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
  });

  test('MT-005 Duplicate username is rejected', async ({ page }) => {
    const existing = await registerCustomer(page.request, 'mt005');
    await page.goto('/register');
    await page.locator('input[name="username"]').fill(existing.username);
    await page.locator('input[name="password"]').fill('Another1!');
    await page.getByRole('button', { name: 'Register' }).click();
    await expect(page.getByText('User already present.')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Register a New Account' })).toBeVisible();
  });

  test('MT-006 Valid login opens dashboard', async ({ page }) => {
    const customer = await registerCustomer(page.request, 'mt006');
    await login(page, customer);
    await expect(page.getByRole('heading', { name: `Welcome, ${customer.username}` })).toBeVisible();
  });

  test('MT-007 Wrong password is rejected', async ({ page }) => {
    const customer = await registerCustomer(page.request, 'mt007');
    await attemptLogin(page, customer.username, 'WrongPass1!');
    await expect(page).toHaveURL(/\/login\?error$/);
    await expect(page.locator('.alert-danger')).toHaveText(INVALID_LOGIN);
  });

  test('MT-008 Unknown username gets the same error', async ({ page }) => {
    const customer = await registerCustomer(page.request, 'mt008');
    await attemptLogin(page, customer.username, 'WrongPass1!');
    const wrongPasswordUrl = new URL(page.url()).pathname + new URL(page.url()).search;
    const wrongPasswordMessage = (await page.locator('.alert-danger').innerText()).trim();

    await attemptLogin(page, uniqueName('mt008_nobody'), 'WrongPass1!');
    expect(new URL(page.url()).pathname + new URL(page.url()).search).toBe(wrongPasswordUrl);
    await expect(page.locator('.alert-danger')).toHaveText(wrongPasswordMessage);
    expect(wrongPasswordMessage).toBe(INVALID_LOGIN);
  });

  test('MT-009 Password is case sensitive', async ({ page }) => {
    const customer = await registerCustomer(page.request, 'mt009');
    const flipped = [...customer.password]
      .map((c) => (c === c.toUpperCase() ? c.toLowerCase() : c.toUpperCase()))
      .join('');
    expect(flipped).not.toBe(customer.password);
    await attemptLogin(page, customer.username, flipped);
    await expect(page).toHaveURL(/\/login\?error$/);
    await expect(page.locator('.alert-danger')).toHaveText(INVALID_LOGIN);
  });

  test('MT-010 Dashboard requires login', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
  });

  test('MT-011 Transaction history requires login', async ({ page }) => {
    await page.goto('/transactions');
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
  });

  test('MT-012 Logout returns to login', async ({ page }) => {
    const customer = await registerCustomer(page.request, 'mt012');
    await login(page, customer);
    await page.getByRole('link', { name: 'Logout' }).click();
    await expect(page).toHaveURL(/\/login\?logout$/);
    await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
  });

  test('MT-013 Session ends after logout', async ({ page }) => {
    const customer = await registerCustomer(page.request, 'mt013');
    await login(page, customer);
    await page.getByRole('link', { name: 'Logout' }).click();
    await expect(page).toHaveURL(/\/login\?logout$/);
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);
  });
});
