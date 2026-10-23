import { expect, test } from '@playwright/test';
import { PASSWORD, login, loginToDashboard, loginUrl, registerViaHttp, uniqueUsername } from './helpers';

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
    await expect(page).toHaveURL(loginUrl);
    await expect(page.getByRole('heading', { name: 'Login', exact: true })).toBeVisible();
  });

  test('MT-004 New customer can register', async ({ page }) => {
    await page.goto('/register');
    await page.locator('input[name="username"]').fill(uniqueUsername('mt004'));
    await page.locator('input[name="password"]').fill(PASSWORD);
    await page.getByRole('button', { name: 'Register' }).click();
    await expect(page).toHaveURL(loginUrl);
    await expect(page.getByRole('heading', { name: 'Login', exact: true })).toBeVisible();
  });

  test('MT-005 Duplicate username is rejected', async ({ page, request }) => {
    const username = uniqueUsername('mt005');
    await registerViaHttp(request, username);
    await page.goto('/register');
    await page.locator('input[name="username"]').fill(username);
    await page.locator('input[name="password"]').fill(PASSWORD);
    await page.getByRole('button', { name: 'Register' }).click();
    await expect(page.getByText('User already present.')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Register a New Account' })).toBeVisible();
  });

  test('MT-006 Valid login opens dashboard', async ({ page, request }) => {
    const username = uniqueUsername('mt006');
    await registerViaHttp(request, username);
    await loginToDashboard(page, username);
    await expect(page.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${username}`);
  });

  test('MT-007 Wrong password is rejected', async ({ page, request }) => {
    const username = uniqueUsername('mt007');
    await registerViaHttp(request, username);
    await login(page, username, 'WrongPassw0rd!');
    await expect(page).toHaveURL(/\/login\?error$/);
    await expect(page.locator('.alert-danger')).toHaveText('Invalid username or password.');
  });

  test('MT-008 Unknown username gets the same error', async ({ page, request }) => {
    const known = uniqueUsername('mt008');
    await registerViaHttp(request, known);
    await login(page, known, 'WrongPassw0rd!');
    const wrongPasswordMessage = (await page.locator('.alert-danger').innerText()).trim();

    await login(page, uniqueUsername('mt008_missing'), PASSWORD);
    await expect(page).toHaveURL(/\/login\?error$/);
    await expect(page.locator('.alert-danger')).toHaveText(wrongPasswordMessage);
    await expect(page.locator('.alert-danger')).toHaveText('Invalid username or password.');
  });

  test('MT-009 Password is case sensitive', async ({ page, request }) => {
    const username = uniqueUsername('mt009');
    await registerViaHttp(request, username, 'CaseSensitive1');
    await login(page, username, 'casesensitive1');
    await expect(page).toHaveURL(/\/login\?error$/);
    await login(page, username, 'CASESENSITIVE1');
    await expect(page).toHaveURL(/\/login\?error$/);
    await expect(page.locator('.alert-danger')).toHaveText('Invalid username or password.');
  });

  test('MT-010 Dashboard requires login', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(loginUrl);
    await expect(page.getByRole('heading', { name: 'Login', exact: true })).toBeVisible();
  });

  test('MT-011 Transaction history requires login', async ({ page }) => {
    await page.goto('/transactions');
    await expect(page).toHaveURL(loginUrl);
    await expect(page.getByRole('heading', { name: 'Login', exact: true })).toBeVisible();
  });

  test('MT-012 Logout returns to login', async ({ page, request }) => {
    const username = uniqueUsername('mt012');
    await registerViaHttp(request, username);
    await loginToDashboard(page, username);
    await page.getByRole('link', { name: 'Logout' }).click();
    await expect(page).toHaveURL(/\/login\?logout$/);
    await expect(page.getByRole('heading', { name: 'Login', exact: true })).toBeVisible();
  });

  test('MT-013 Session ends after logout', async ({ page, request }) => {
    const username = uniqueUsername('mt013');
    await registerViaHttp(request, username);
    await loginToDashboard(page, username);
    await page.getByRole('link', { name: 'Logout' }).click();
    await expect(page).toHaveURL(/\/login\?logout$/);
    await page.goto('/dashboard');
    await expect(page).toHaveURL(loginUrl);
    await expect(page.getByText(`Welcome, ${username}`)).toHaveCount(0);
  });
});
