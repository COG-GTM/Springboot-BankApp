import { test, expect } from '@playwright/test';
import {
  balanceOf, deposit, errorMessage, expectBalance, historyRow, historyRows, login, newCustomerSession, openHistory,
  openPanel, recordPosts, registerCustomer, transfer, uniqueName,
} from './helpers';

test.describe('Transfer', () => {
  test('MT-036 Transfer debits the sender', async ({ page }) => {
    await newCustomerSession(page, 'mt036s');
    const recipient = await registerCustomer(page.request, 'mt036r');
    await deposit(page, 100);
    await transfer(page, recipient.username, 30);
    await expect(errorMessage(page)).toHaveCount(0);
    await expectBalance(page, '70.00');
  });

  test('MT-037 Transfer credits the recipient', async ({ page, browser }) => {
    await newCustomerSession(page, 'mt037s');
    const recipient = await registerCustomer(page.request, 'mt037r');
    await deposit(page, 100);
    await transfer(page, recipient.username, 30);
    await expectBalance(page, '70.00');
    expect(await balanceOf(browser, recipient)).toBe('Current Balance: $30.00');
  });

  test('MT-038 Sender history shows the transfer', async ({ page }) => {
    await newCustomerSession(page, 'mt038s');
    const recipient = await registerCustomer(page.request, 'mt038r');
    await deposit(page, 100);
    await transfer(page, recipient.username, 30);
    await openHistory(page);
    const row = historyRow(page, `Transfer Out to ${recipient.username}`);
    await expect(row).toHaveCount(1);
    await expect(row.locator('td').nth(2)).toContainText('30.00');
  });

  test('MT-039 Recipient history shows the transfer', async ({ page, browser }) => {
    const sender = await newCustomerSession(page, 'mt039s');
    const recipient = await registerCustomer(page.request, 'mt039r');
    await deposit(page, 100);
    await transfer(page, recipient.username, 30);
    await expectBalance(page, '70.00');

    const context = await browser.newContext();
    const recipientPage = await context.newPage();
    await login(recipientPage, recipient);
    await openHistory(recipientPage);
    const row = historyRow(recipientPage, `Transfer In from ${sender.username}`);
    await expect(row).toHaveCount(1);
    await expect(row.locator('td').nth(2)).toContainText('30.00');
    await context.close();
  });

  test('MT-040 Customer can transfer the full balance', async ({ page, browser }) => {
    await newCustomerSession(page, 'mt040s');
    const recipient = await registerCustomer(page.request, 'mt040r');
    await deposit(page, 100);
    await transfer(page, recipient.username, 100);
    await expect(errorMessage(page)).toHaveCount(0);
    await expectBalance(page, '0.00');
    expect(await balanceOf(browser, recipient)).toBe('Current Balance: $100.00');
  });

  test('MT-041 Transfer above balance is rejected', async ({ page, browser }) => {
    await newCustomerSession(page, 'mt041s');
    const recipient = await registerCustomer(page.request, 'mt041r');
    await deposit(page, 100);
    await transfer(page, recipient.username, 150);
    await expect(errorMessage(page)).toHaveText('Insufficient funds');
    await page.goto('/dashboard');
    await expectBalance(page, '100.00');
    expect(await balanceOf(browser, recipient)).toBe('Current Balance: $0.00');
  });

  // The app checks funds before it looks up the recipient, so the sender must hold at least
  // the amount for 'Recipient account not found' to be reachable.
  test('MT-042 Transfer to unknown customer is rejected', async ({ page }) => {
    await newCustomerSession(page, 'mt042');
    await deposit(page, 50);
    await transfer(page, uniqueName('mt042_nobody'), 10);
    await expect(errorMessage(page)).toHaveText('Recipient account not found');
    await page.goto('/dashboard');
    await expectBalance(page, '50.00');
  });

  test('MT-043 Recipient is required', async ({ page }) => {
    await newCustomerSession(page, 'mt043');
    const posts = recordPosts(page);
    const form = await openPanel(page, 'Transfer Money');
    await form.locator('input[name="amount"]').fill('10');
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await form.locator('input[name="toUsername"]').evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    await expect(page).toHaveURL(/\/dashboard$/);
    expect(posts).toEqual([]);
  });

  test('MT-044 Amount is required', async ({ page }) => {
    await newCustomerSession(page, 'mt044');
    const recipient = await registerCustomer(page.request, 'mt044r');
    const posts = recordPosts(page);
    const form = await openPanel(page, 'Transfer Money');
    await form.locator('input[name="toUsername"]').fill(recipient.username);
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await form.locator('input[name="amount"]').evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    await expect(page).toHaveURL(/\/dashboard$/);
    expect(posts).toEqual([]);
  });

  test('MT-045 Multiple transfers add up', async ({ page, browser }) => {
    await newCustomerSession(page, 'mt045s');
    const recipient = await registerCustomer(page.request, 'mt045r');
    await deposit(page, 100);
    await transfer(page, recipient.username, 20);
    await transfer(page, recipient.username, 30);
    await expectBalance(page, '50.00');
    expect(await balanceOf(browser, recipient)).toBe('Current Balance: $50.00');
  });

  test('MT-046 Rejected transfer leaves no history', async ({ page }) => {
    await newCustomerSession(page, 'mt046');
    await deposit(page, 50);
    await transfer(page, uniqueName('mt046_nobody'), 10);
    await expect(errorMessage(page)).toHaveText('Recipient account not found');
    await openHistory(page);
    await expect(historyRows(page).filter({ hasText: 'Transfer' })).toHaveCount(0);
    await expect(historyRows(page)).toHaveCount(1);
  });
});
