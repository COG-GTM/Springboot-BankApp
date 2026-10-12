import { APIRequestContext, Locator, Page, expect } from '@playwright/test';

export const PASSWORD = 'Passw0rd!';

export function uniqueUser(prefix = 'cust'): string {
  return `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 7)}`;
}

export async function registerViaApi(request: APIRequestContext, username: string, password = PASSWORD) {
  const res = await request.post('/register', { form: { username, password }, maxRedirects: 0 });
  expect(res.status(), `register ${username}`).toBe(302);
}

type Panel = 'deposit' | 'withdraw' | 'transfer';

export class BankApp {
  constructor(readonly page: Page) {}

  async gotoLogin() {
    await this.page.goto('/login');
  }

  async login(username: string, password = PASSWORD) {
    await this.gotoLogin();
    await this.page.locator('input[name="username"]').fill(username);
    await this.page.locator('input[name="password"]').fill(password);
    await this.page.getByRole('button', { name: 'Login' }).click();
  }

  async register(username: string, password = PASSWORD) {
    await this.page.goto('/register');
    await this.page.locator('input[name="username"]').fill(username);
    await this.page.locator('input[name="password"]').fill(password);
    await this.page.getByRole('button', { name: 'Register' }).click();
  }

  get balance(): Locator {
    return this.page.locator('h3', { hasText: 'Current Balance' });
  }

  get error(): Locator {
    return this.page.locator('p.error');
  }

  async expectBalance(amount: string) {
    await expect(this.balance).toHaveText(`Current Balance: $${amount}`);
  }

  form(panel: Panel): Locator {
    return this.page.locator(`form[action="/${panel}"]`);
  }

  async openPanel(panel: Panel) {
    await this.page.waitForFunction(() => typeof (window as any).jQuery?.fn?.collapse === 'function');
    await this.page.locator(`button[data-target="#${panel}Form"]`).click();
    await expect(this.page.locator(`#${panel}Form`)).toHaveClass(/show/);
  }

  async submit(panel: Panel, fields: Record<string, string>) {
    await this.openPanel(panel);
    const form = this.form(panel);
    for (const [name, value] of Object.entries(fields)) {
      await form.locator(`input[name="${name}"]`).fill(value);
    }
    await form.getByRole('button', { name: 'Submit' }).click();
  }

  deposit(amount: string) {
    return this.submit('deposit', { amount });
  }

  withdraw(amount: string) {
    return this.submit('withdraw', { amount });
  }

  transfer(toUsername: string, amount: string) {
    return this.submit('transfer', { toUsername, amount });
  }

  async logout() {
    await this.page.getByRole('link', { name: 'Logout' }).click();
    await expect(this.page).toHaveURL(/\/login\?logout/);
  }

  async openTransactions() {
    await this.page.goto('/transactions');
    await expect(this.page.getByRole('heading', { name: 'Transaction History' })).toBeVisible();
  }

  historyRows(): Locator {
    return this.page.locator('table tbody tr');
  }

  historyAmount(type: string): Locator {
    return this.historyRow(type).locator('td').nth(2);
  }

  historyRow(type: string): Locator {
    return this.historyRows().filter({ has: this.page.locator('td', { hasText: type }) });
  }
}
