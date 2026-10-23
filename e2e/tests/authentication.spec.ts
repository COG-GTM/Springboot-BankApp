import { test, expect, login, logout, newCustomer, registerViaApi, uniqueUsername, PASSWORD } from './fixtures';

const loginError = 'Invalid username or password.';

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
    await page.locator('input[name="username"]').fill(uniqueUsername('reg'));
    await page.locator('input[name="password"]').fill(PASSWORD);
    await page.getByRole('button', { name: 'Register' }).click();
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
  });

  test('MT-005 Duplicate username is rejected', async ({ page }) => {
    const username = uniqueUsername('dup');
    await registerViaApi(page, username);
    await page.goto('/register');
    await page.locator('input[name="username"]').fill(username);
    await page.locator('input[name="password"]').fill('Other0ne!');
    await page.getByRole('button', { name: 'Register' }).click();
    await expect(page.getByText('User already present.')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Register a New Account' })).toBeVisible();
  });

  test('MT-006 Valid login opens dashboard', async ({ page }) => {
    const username = uniqueUsername('login');
    await registerViaApi(page, username);
    await login(page, username);
    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole('heading', { level: 2 })).toHaveText(`Welcome, ${username}`);
  });

  test('MT-007 Wrong password is rejected', async ({ page }) => {
    const username = uniqueUsername('badpw');
    await registerViaApi(page, username);
    await login(page, username, 'WrongPass1!');
    await expect(page).toHaveURL(/\/login\?error/);
    await expect(page.getByText(loginError)).toBeVisible();
  });

  test('MT-008 Unknown username gets the same error', async ({ page }) => {
    const known = uniqueUsername('known');
    await registerViaApi(page, known);
    await login(page, known, 'WrongPass1!');
    const knownAlert = (await page.locator('.alert-danger').innerText()).trim();

    await login(page, uniqueUsername('ghost'), PASSWORD);
    await expect(page).toHaveURL(/\/login\?error/);
    await expect(page.locator('.alert-danger')).toHaveText(knownAlert);
    expect(knownAlert).toBe(loginError);
  });

  test('MT-009 Password is case sensitive', async ({ page }) => {
    const username = uniqueUsername('case');
    await registerViaApi(page, username);
    await login(page, username, PASSWORD.toUpperCase());
    await expect(page).toHaveURL(/\/login\?error/);
    await login(page, username, PASSWORD.toLowerCase());
    await expect(page).toHaveURL(/\/login\?error/);
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
    await newCustomer(page);
    await page.getByRole('link', { name: 'Logout' }).click();
    await expect(page).toHaveURL(/\/login\?logout$/);
    await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
  });

  test('MT-013 Session ends after logout', async ({ page }) => {
    await newCustomer(page);
    await logout(page);
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);
  });
});
