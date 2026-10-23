import { APIRequestContext, Browser, Page, expect } from '@playwright/test';

export const PASSWORD = 'Passw0rd!';

let sequence = 0;

/** Every test creates its own customers, so tests never share balances and can run in parallel. */
export function uniqueUsername(prefix = 'qa'): string {
  sequence += 1;
  const random = Math.random().toString(36).slice(2, 7);
  return `${prefix}_${Date.now().toString(36)}_${process.pid}_${sequence}_${random}`;
}

export async function registerViaHttp(request: APIRequestContext, username: string, password = PASSWORD) {
  const response = await request.post('/register', { form: { username, password }, maxRedirects: 0 });
  expect(response.status(), `register ${username}`).toBe(302);
  expect(response.headers()['location']).toMatch(/\/login$/);
}

export async function login(page: Page, username: string, password = PASSWORD) {
  await page.goto('/login');
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[name="password"]').fill(password);
  await page.getByRole('button', { name: 'Login' }).click();
}

export async function loginToDashboard(page: Page, username: string, password = PASSWORD) {
  await login(page, username, password);
  await expect(page).toHaveURL(/\/dashboard$/);
}

export async function newCustomer(page: Page, request: APIRequestContext, prefix = 'qa'): Promise<string> {
  const username = uniqueUsername(prefix);
  await registerViaHttp(request, username);
  await loginToDashboard(page, username);
  return username;
}

/** Opens a second, fully separate browser session (own cookies) logged in as `username`. */
export async function sessionFor(browser: Browser, username: string): Promise<Page> {
  const context = await browser.newContext();
  const page = await context.newPage();
  await loginToDashboard(page, username);
  return page;
}

export async function expectBalance(page: Page, amount: string) {
  await expect(page.locator('h3')).toHaveText(`Current Balance: $${amount}`);
}

type Panel = 'deposit' | 'withdraw' | 'transfer';
const panelButton: Record<Panel, string> = { deposit: 'Deposit', withdraw: 'Withdraw', transfer: 'Transfer Money' };

export function panelForm(page: Page, panel: Panel) {
  return page.locator(`#${panel}Form form`);
}

/** Dashboard panels are Bootstrap collapses loaded from a CDN; clicking before the plugin loads does nothing. */
export async function openPanel(page: Page, panel: Panel) {
  await page.waitForFunction(() => {
    const jq = (window as unknown as { jQuery?: { fn?: { collapse?: unknown } } }).jQuery;
    return !!jq?.fn?.collapse;
  });
  await page.getByRole('button', { name: panelButton[panel], exact: true }).click();
  await expect(page.locator(`#${panel}Form`)).toHaveClass(/\bshow\b/);
  return panelForm(page, panel);
}

async function submitAndWaitForLoad(page: Page, form: ReturnType<typeof panelForm>) {
  await Promise.all([page.waitForEvent('load'), form.getByRole('button', { name: 'Submit' }).click()]);
}

export async function deposit(page: Page, amount: string) {
  const form = await openPanel(page, 'deposit');
  await form.locator('input[name="amount"]').fill(amount);
  await submitAndWaitForLoad(page, form);
}

export async function withdraw(page: Page, amount: string) {
  const form = await openPanel(page, 'withdraw');
  await form.locator('input[name="amount"]').fill(amount);
  await submitAndWaitForLoad(page, form);
}

export async function transfer(page: Page, toUsername: string, amount: string) {
  const form = await openPanel(page, 'transfer');
  await form.locator('input[name="toUsername"]').fill(toUsername);
  await form.locator('input[name="amount"]').fill(amount);
  await submitAndWaitForLoad(page, form);
}

/** Records every POST to `path` so a test can prove the browser never submitted the form. */
export function recordPosts(page: Page, path: string): string[] {
  const posts: string[] = [];
  page.on('request', (req) => {
    if (req.method() === 'POST' && new URL(req.url()).pathname === path) posts.push(req.url());
  });
  return posts;
}

export async function openTransactions(page: Page) {
  await page.goto('/transactions');
  await expect(page.getByRole('heading', { name: 'Transaction History' })).toBeVisible();
}

export function historyRows(page: Page) {
  return page.locator('table tbody tr');
}

export async function historyTypesAndAmounts(page: Page): Promise<Array<{ type: string; amount: string }>> {
  const rows = historyRows(page);
  const count = await rows.count();
  const result: Array<{ type: string; amount: string }> = [];
  for (let i = 0; i < count; i++) {
    const cells = rows.nth(i).locator('td');
    result.push({ type: (await cells.nth(1).innerText()).trim(), amount: (await cells.nth(2).innerText()).trim() });
  }
  return result;
}

export const loginUrl = /\/login(;jsessionid=[^?]*)?(\?.*)?$/;
