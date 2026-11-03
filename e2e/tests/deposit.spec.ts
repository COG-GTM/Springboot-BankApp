import { test, expect } from '@playwright/test';
import { deposit, expectBalance, historyRow, newCustomerSession, openHistory, openPanel, recordPosts } from './helpers';

test.describe('Deposit', () => {
  test('MT-022 Deposit increases balance', async ({ page }) => {
    await newCustomerSession(page, 'mt022');
    await deposit(page, 100);
    await expectBalance(page, '100.00');
  });

  test('MT-023 Deposits add up', async ({ page }) => {
    await newCustomerSession(page, 'mt023');
    await deposit(page, 100);
    await deposit(page, 250);
    await expectBalance(page, '350.00');
  });

  test('MT-024 Deposit appears in history', async ({ page }) => {
    await newCustomerSession(page, 'mt024');
    await deposit(page, 100);
    await openHistory(page);
    const row = historyRow(page, 'Deposit');
    await expect(row).toHaveCount(1);
    await expect(row.locator('td').nth(2)).toContainText('100.00');
  });

  test('MT-025 Empty deposit is blocked', async ({ page }) => {
    await newCustomerSession(page, 'mt025');
    const posts = recordPosts(page);
    const form = await openPanel(page, 'Deposit');
    await form.getByRole('button', { name: 'Submit' }).click();
    const amount = form.locator('input[name="amount"]');
    expect(await amount.evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    await expect(page).toHaveURL(/\/dashboard$/);
    expect(posts).toEqual([]);
    await page.reload();
    await expectBalance(page, '0.00');
  });

  test('MT-026 Deposit field accepts whole dollars only', async ({ page }) => {
    await newCustomerSession(page, 'mt026');
    const posts = recordPosts(page);
    const form = await openPanel(page, 'Deposit');
    const amount = form.locator('input[name="amount"]');
    await amount.fill('10.50');
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await amount.evaluate((el: HTMLInputElement) => el.validity.stepMismatch)).toBe(true);
    await expect(page).toHaveURL(/\/dashboard$/);
    expect(posts).toEqual([]);
    await page.reload();
    await expectBalance(page, '0.00');
  });

  test('MT-027 Large deposit is handled', async ({ page }) => {
    await newCustomerSession(page, 'mt027');
    await deposit(page, 1000000);
    await expectBalance(page, '1000000.00');
  });

  test('MT-028 Letters cannot be typed into amount', async ({ page }) => {
    await newCustomerSession(page, 'mt028');
    const form = await openPanel(page, 'Deposit');
    const amount = form.locator('input[name="amount"]');
    await amount.click();
    await amount.pressSequentially('abc');
    await expect(amount).toHaveValue('');
  });
});
