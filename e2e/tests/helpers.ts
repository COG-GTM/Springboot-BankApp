import { expect, Page, Browser } from '@playwright/test';

export const PASSWORD = 'Passw0rd!';

let counter = 0;
export function uniqueUsername(prefix = 'u'): string {
  counter += 1;
  const rand = Math.random().toString(36).slice(2, 8);
  return `${prefix}_${Date.now().toString(36)}_${process.pid}_${counter}_${rand}`;
}

export async function register(page: Page, username: string, password = PASSWORD) {
  await page.goto('/register');
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[name="password"]').fill(password);
  await page.getByRole('button', { name: 'Register' }).click();
}

export async function login(page: Page, username: string, password = PASSWORD) {
  await page.goto('/login');
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[name="password"]').fill(password);
  await page.getByRole('button', { name: 'Login' }).click();
}

export async function newCustomer(page: Page, prefix = 'u'): Promise<string> {
  const username = uniqueUsername(prefix);
  await register(page, username);
  await expect(page).toHaveURL(/\/login$/);
  await login(page, username);
  await expect(page).toHaveURL(/\/dashboard$/);
  return username;
}

/** Registers a second customer in an isolated browser context so the caller's session is untouched. */
export async function registerOther(browser: Browser, prefix = 'r'): Promise<string> {
  const context = await browser.newContext();
  const page = await context.newPage();
  const username = uniqueUsername(prefix);
  await register(page, username);
  await expect(page).toHaveURL(/\/login$/);
  await context.close();
  return username;
}

export function balance(page: Page) {
  return page.getByRole('heading', { level: 3 });
}

export async function expectBalance(page: Page, amount: string) {
  await expect(balance(page)).toHaveText(`Current Balance: $${amount}`);
}

export function panel(page: Page, id: 'depositForm' | 'withdrawForm' | 'transferForm') {
  return page.locator(`#${id}`);
}

async function openPanel(page: Page, id: 'depositForm' | 'withdrawForm' | 'transferForm') {
  await page.locator(`button[data-target="#${id}"]`).click();
  await expect(panel(page, id)).toBeVisible();
}

export async function deposit(page: Page, amount: string) {
  await page.goto('/dashboard');
  await openPanel(page, 'depositForm');
  await panel(page, 'depositForm').locator('input[name="amount"]').fill(amount);
  await panel(page, 'depositForm').getByRole('button', { name: 'Submit' }).click();
}

export async function withdraw(page: Page, amount: string) {
  await page.goto('/dashboard');
  await openPanel(page, 'withdrawForm');
  await panel(page, 'withdrawForm').locator('input[name="amount"]').fill(amount);
  await panel(page, 'withdrawForm').getByRole('button', { name: 'Submit' }).click();
}

export async function transfer(page: Page, toUsername: string, amount: string) {
  await page.goto('/dashboard');
  await openPanel(page, 'transferForm');
  const form = panel(page, 'transferForm');
  await form.locator('input[name="toUsername"]').fill(toUsername);
  await form.locator('input[name="amount"]').fill(amount);
  await form.getByRole('button', { name: 'Submit' }).click();
}

export { openPanel };

export function historyRows(page: Page) {
  return page.locator('table tbody tr');
}

/** Returns [type, amount] pairs from the Transaction History table. */
export async function historyEntries(page: Page): Promise<Array<{ type: string; amount: string; date: string }>> {
  await page.goto('/transactions');
  await expect(page.getByRole('heading', { name: 'Transaction History' })).toBeVisible();
  const rows = historyRows(page);
  const n = await rows.count();
  const out: Array<{ type: string; amount: string; date: string }> = [];
  for (let i = 0; i < n; i++) {
    const cells = rows.nth(i).locator('td');
    out.push({
      type: (await cells.nth(1).innerText()).trim(),
      amount: (await cells.nth(2).innerText()).trim(),
      date: (await cells.nth(3).innerText()).trim(),
    });
  }
  return out;
}

/** Records every POST to the given path so tests can assert a form was not submitted. */
export function trackPosts(page: Page, path: string): string[] {
  const seen: string[] = [];
  page.on('request', (req) => {
    if (req.method() === 'POST' && new URL(req.url()).pathname === path) seen.push(req.url());
  });
  return seen;
}

export async function balanceOf(browser: Browser, username: string): Promise<string> {
  const context = await browser.newContext();
  const page = await context.newPage();
  await login(page, username);
  await expect(page).toHaveURL(/\/dashboard$/);
  const text = (await balance(page).innerText()).trim();
  await context.close();
  return text;
}
