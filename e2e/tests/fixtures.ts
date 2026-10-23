import { test as base, expect, type Browser, type Page } from '@playwright/test';

export const PASSWORD = 'Passw0rd!';

let counter = 0;
export function uniqueUsername(prefix = 'qa'): string {
  counter += 1;
  return `${prefix}_${Date.now().toString(36)}_${process.pid}_${counter}`;
}

export async function registerViaApi(page: Page, username: string, password = PASSWORD): Promise<void> {
  const res = await page.request.post('/register', {
    form: { username, password },
    maxRedirects: 0,
  });
  expect(res.status(), `register ${username}`).toBe(302);
}

export async function login(page: Page, username: string, password = PASSWORD): Promise<void> {
  await page.goto('/login');
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[name="password"]').fill(password);
  await page.getByRole('button', { name: 'Login' }).click();
}

export async function logout(page: Page): Promise<void> {
  await page.getByRole('link', { name: 'Logout' }).click();
  await expect(page).toHaveURL(/\/login\?logout/);
}

export async function newCustomer(page: Page, prefix = 'qa'): Promise<string> {
  const username = uniqueUsername(prefix);
  await registerViaApi(page, username);
  await login(page, username);
  await expect(page).toHaveURL(/\/dashboard$/);
  return username;
}

/** Opens a fresh browser context (separate cookies) logged in as `username`. */
export async function sessionFor(browser: Browser, baseURL: string | undefined, username: string): Promise<Page> {
  const context = await browser.newContext({ baseURL });
  const page = await context.newPage();
  await login(page, username);
  await expect(page).toHaveURL(/\/dashboard$/);
  return page;
}

export const balance = (page: Page) => page.getByRole('heading', { level: 3, name: /^Current Balance:/ });

export async function expectBalance(page: Page, amount: string): Promise<void> {
  await expect(balance(page)).toHaveText(`Current Balance: $${amount}`);
}

export const errorMessage = (page: Page) => page.locator('p.error');

const panelButton = { deposit: 'Deposit', withdraw: 'Withdraw', transfer: 'Transfer Money' } as const;
const panelId = { deposit: '#depositForm', withdraw: '#withdrawForm', transfer: '#transferForm' } as const;
export type Panel = keyof typeof panelId;

export async function openPanel(page: Page, panel: Panel) {
  // The panels are Bootstrap collapses loaded from a CDN at the end of <body>; a click before the plugin is
  // wired up does nothing, so wait for it (a login redirect can resolve toHaveURL before the scripts load).
  await page.waitForFunction(() => typeof (window as any).jQuery?.fn?.collapse === 'function');
  await page.getByRole('button', { name: panelButton[panel], exact: true }).click();
  const form = page.locator(`${panelId[panel]} form`);
  await expect(form).toBeVisible();
  return form;
}

async function submitAmount(page: Page, panel: Panel, amount: string, toUsername?: string): Promise<void> {
  await page.goto('/dashboard');
  const form = await openPanel(page, panel);
  if (toUsername !== undefined) await form.locator('input[name="toUsername"]').fill(toUsername);
  await form.locator('input[name="amount"]').fill(amount);
  await form.getByRole('button', { name: 'Submit' }).click();
  await page.waitForLoadState('domcontentloaded');
}

export const deposit = (page: Page, amount: string) => submitAmount(page, 'deposit', amount);
export const withdraw = (page: Page, amount: string) => submitAmount(page, 'withdraw', amount);
export const transfer = (page: Page, toUsername: string, amount: string) =>
  submitAmount(page, 'transfer', amount, toUsername);

export type HistoryRow = { id: string; type: string; amount: string; date: string };

export async function history(page: Page): Promise<HistoryRow[]> {
  await page.goto('/transactions');
  await expect(page.getByRole('heading', { name: 'Transaction History' })).toBeVisible();
  const rows = page.locator('table tbody tr');
  const out: HistoryRow[] = [];
  for (const row of await rows.all()) {
    const cells = (await row.locator('td').allInnerTexts()).map((c) => c.trim());
    out.push({ id: cells[0], type: cells[1], amount: cells[2], date: cells[3] });
  }
  return out;
}

/** Records every POST the page sends to `path`, so a test can assert a form was never submitted. */
export function trackPosts(page: Page, path: string): string[] {
  const seen: string[] = [];
  page.on('request', (req) => {
    if (req.method() === 'POST' && new URL(req.url()).pathname === path) seen.push(req.url());
  });
  return seen;
}

export const test = base;
export { expect };
