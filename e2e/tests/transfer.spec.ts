import {
  test, expect, deposit, errorMessage, expectBalance, history, newCustomer, openPanel,
  registerViaApi, sessionFor, trackPosts, transfer, uniqueUsername,
} from './fixtures';
import type { Page } from '@playwright/test';

/** Registers a recipient (no login) and returns its username. */
async function recipient(page: Page): Promise<string> {
  const username = uniqueUsername('payee');
  await registerViaApi(page, username);
  return username;
}

test.describe('Transfer', () => {
  test('MT-036 Transfer debits the sender', async ({ page }) => {
    const payee = await recipient(page);
    await newCustomer(page, 'sender');
    await deposit(page, '100');
    await transfer(page, payee, '30');
    await expect(errorMessage(page)).toHaveCount(0);
    await expectBalance(page, '70.00');
  });

  // The plan says "After MT-036"; each test repeats MT-036's steps so it can run on its own.
  test('MT-037 Transfer credits the recipient', async ({ page, browser, baseURL }) => {
    const payee = await recipient(page);
    await newCustomer(page, 'sender');
    await deposit(page, '100');
    await transfer(page, payee, '30');
    const payeePage = await sessionFor(browser, baseURL, payee);
    await expectBalance(payeePage, '30.00');
  });

  test('MT-038 Sender history shows the transfer', async ({ page }) => {
    const payee = await recipient(page);
    await newCustomer(page, 'sender');
    await deposit(page, '100');
    await transfer(page, payee, '30');
    const rows = await history(page);
    expect(rows.filter((r) => r.type.startsWith('Transfer')).map(({ type, amount }) => ({ type, amount }))).toEqual([
      { type: `Transfer Out to ${payee}`, amount: '30.00' },
    ]);
  });

  test('MT-039 Recipient history shows the transfer', async ({ page, browser, baseURL }) => {
    const payee = await recipient(page);
    const sender = await newCustomer(page, 'sender');
    await deposit(page, '100');
    await transfer(page, payee, '30');
    const rows = await history(await sessionFor(browser, baseURL, payee));
    expect(rows.map(({ type, amount }) => ({ type, amount }))).toEqual([
      { type: `Transfer In from ${sender}`, amount: '30.00' },
    ]);
  });

  test('MT-040 Customer can transfer the full balance', async ({ page, browser, baseURL }) => {
    const payee = await recipient(page);
    await newCustomer(page, 'sender');
    await deposit(page, '100');
    await transfer(page, payee, '100');
    await expect(errorMessage(page)).toHaveCount(0);
    await expectBalance(page, '0.00');
    await expectBalance(await sessionFor(browser, baseURL, payee), '100.00');
  });

  test('MT-041 Transfer above balance is rejected', async ({ page, browser, baseURL }) => {
    const payee = await recipient(page);
    await newCustomer(page, 'sender');
    await deposit(page, '100');
    await transfer(page, payee, '150');
    await expect(errorMessage(page)).toHaveText('Insufficient funds');
    await page.goto('/dashboard');
    await expectBalance(page, '100.00');
    await expectBalance(await sessionFor(browser, baseURL, payee), '0.00');
  });

  test('MT-042 Transfer to unknown customer is rejected', async ({ page }) => {
    await newCustomer(page, 'sender');
    await deposit(page, '100');
    await transfer(page, uniqueUsername('ghost'), '10');
    await expect(errorMessage(page)).toHaveText('Recipient account not found');
    await page.goto('/dashboard');
    await expectBalance(page, '100.00');
  });

  test('MT-043 Recipient is required', async ({ page }) => {
    await newCustomer(page, 'sender');
    const posts = trackPosts(page, '/transfer');
    const form = await openPanel(page, 'transfer');
    await form.locator('input[name="amount"]').fill('10');
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await form.locator('input[name="toUsername"]').evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    await expect(page).toHaveURL(/\/dashboard$/);
    expect(posts).toEqual([]);
  });

  test('MT-044 Amount is required', async ({ page }) => {
    const payee = await recipient(page);
    await newCustomer(page, 'sender');
    const posts = trackPosts(page, '/transfer');
    const form = await openPanel(page, 'transfer');
    await form.locator('input[name="toUsername"]').fill(payee);
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await form.locator('input[name="amount"]').evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    await expect(page).toHaveURL(/\/dashboard$/);
    expect(posts).toEqual([]);
  });

  test('MT-045 Multiple transfers add up', async ({ page, browser, baseURL }) => {
    const payee = await recipient(page);
    await newCustomer(page, 'sender');
    await deposit(page, '100');
    await transfer(page, payee, '20');
    await transfer(page, payee, '30');
    await expectBalance(page, '50.00');
    await expectBalance(await sessionFor(browser, baseURL, payee), '50.00');
  });

  test('MT-046 Rejected transfer leaves no history', async ({ page }) => {
    await newCustomer(page, 'sender');
    await deposit(page, '100');
    await transfer(page, uniqueUsername('ghost'), '10');
    await expect(errorMessage(page)).toHaveText('Recipient account not found');
    const rows = await history(page);
    expect(rows.filter((r) => r.type.startsWith('Transfer'))).toEqual([]);
  });
});

