import { APIRequestContext, Browser, Page, expect } from '@playwright/test';

export const PASSWORD = 'Passw0rd!';

export interface Customer {
  username: string;
  password: string;
}

let counter = 0;

export function uniqueName(prefix: string): string {
  counter += 1;
  const rand = Math.random().toString(36).slice(2, 8);
  return `${prefix}_${Date.now().toString(36)}${process.pid.toString(36)}${counter}${rand}`.toLowerCase();
}

/** Creates a customer through the real registration endpoint (no UI) for test setup. */
export async function registerCustomer(request: APIRequestContext, prefix = 'qa'): Promise<Customer> {
  const customer = { username: uniqueName(prefix), password: PASSWORD };
  const res = await request.post('/register', { form: customer, maxRedirects: 0 });
  expect(res.status(), `register ${customer.username}`).toBe(302);
  expect(res.headers()['location']).toMatch(/\/login$/);
  return customer;
}

export async function login(page: Page, customer: Customer): Promise<void> {
  await page.goto('/login');
  await page.locator('input[name="username"]').fill(customer.username);
  await page.locator('input[name="password"]').fill(customer.password);
  await page.getByRole('button', { name: 'Login' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
}

export async function newCustomerSession(page: Page, prefix = 'qa'): Promise<Customer> {
  const customer = await registerCustomer(page.request, prefix);
  await login(page, customer);
  return customer;
}

export function balance(page: Page) {
  return page.locator('h3', { hasText: 'Current Balance:' });
}

export async function expectBalance(page: Page, amount: string): Promise<void> {
  await expect(balance(page)).toHaveText(`Current Balance: $${amount}`);
}

export function errorMessage(page: Page) {
  return page.locator('p.error');
}

type Panel = 'Deposit' | 'Withdraw' | 'Transfer Money';
const panelIds: Record<Panel, string> = {
  Deposit: '#depositForm',
  Withdraw: '#withdrawForm',
  'Transfer Money': '#transferForm',
};

/** Opens a Bootstrap collapse panel. Bootstrap JS comes from a CDN, so wait for it before clicking. */
export async function openPanel(page: Page, panel: Panel) {
  await page.waitForFunction(() => {
    const w = window as unknown as { jQuery?: { fn?: { collapse?: unknown } } };
    return typeof w.jQuery?.fn?.collapse === 'function';
  });
  const form = page.locator(panelIds[panel]);
  if (!(await form.isVisible())) {
    await page.getByRole('button', { name: panel, exact: true }).click();
  }
  await expect(form).toBeVisible();
  return form;
}

async function submitAmount(page: Page, panel: 'Deposit' | 'Withdraw', amount: string | number) {
  const form = await openPanel(page, panel);
  await form.locator('input[name="amount"]').fill(String(amount));
  await Promise.all([
    page.waitForURL(/\/(dashboard|withdraw)$/),
    form.getByRole('button', { name: 'Submit' }).click(),
  ]);
}

export async function deposit(page: Page, amount: string | number) {
  await submitAmount(page, 'Deposit', amount);
  await expect(page).toHaveURL(/\/dashboard$/);
}

export async function withdraw(page: Page, amount: string | number) {
  await submitAmount(page, 'Withdraw', amount);
}

export async function transfer(page: Page, toUsername: string, amount: string | number) {
  const form = await openPanel(page, 'Transfer Money');
  await form.locator('input[name="toUsername"]').fill(toUsername);
  await form.locator('input[name="amount"]').fill(String(amount));
  await Promise.all([
    page.waitForURL(/\/(dashboard|transfer)$/),
    form.getByRole('button', { name: 'Submit' }).click(),
  ]);
}

/** Records every POST the page sends, to prove a browser-blocked form never reached the server. */
export function recordPosts(page: Page): string[] {
  const posts: string[] = [];
  page.on('request', (req) => {
    if (req.method() === 'POST') posts.push(new URL(req.url()).pathname);
  });
  return posts;
}

export function historyRows(page: Page) {
  return page.locator('table tbody tr');
}

export function historyRow(page: Page, type: string) {
  return historyRows(page).filter({ has: page.locator('td:nth-child(2)', { hasText: new RegExp(`^${escapeRegExp(type)}$`) }) });
}

export async function openHistory(page: Page) {
  await page.goto('/transactions');
  await expect(page.getByRole('heading', { name: 'Transaction History' })).toBeVisible();
}

/** Logs a customer in on a separate browser context and returns their dashboard balance text. */
export async function balanceOf(browser: Browser, customer: Customer): Promise<string> {
  const context = await browser.newContext();
  try {
    const page = await context.newPage();
    await login(page, customer);
    return (await balance(page).innerText()).trim();
  } finally {
    await context.close();
  }
}

export function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
