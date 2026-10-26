import { test, expect } from '@playwright/test';
import { deposit, expectBalance, historyEntries, newCustomer, openPanel, panel, trackPosts } from './helpers';

test.describe('Deposit', () => {
  test('MT-022 Deposit increases balance', async ({ page }) => {
    await newCustomer(page, 'dep');
    await deposit(page, '100');
    await expect(page).toHaveURL(/\/dashboard$/);
    await expectBalance(page, '100.00');
  });

  test('MT-023 Deposits add up', async ({ page }) => {
    await newCustomer(page, 'dep2');
    await deposit(page, '100');
    await expectBalance(page, '100.00');
    await deposit(page, '250');
    await expectBalance(page, '350.00');
  });

  test('MT-024 Deposit appears in history', async ({ page }) => {
    await newCustomer(page, 'dephist');
    await deposit(page, '100');
    await expectBalance(page, '100.00');
    const rows = await historyEntries(page);
    expect(rows).toEqual([expect.objectContaining({ type: 'Deposit', amount: '100.00' })]);
  });

  test('MT-025 Empty deposit is blocked', async ({ page }) => {
    await newCustomer(page, 'depempty');
    const posts = trackPosts(page, '/deposit');
    await openPanel(page, 'depositForm');
    const amount = panel(page, 'depositForm').locator('input[name="amount"]');
    await panel(page, 'depositForm').getByRole('button', { name: 'Submit' }).click();
    expect(await amount.evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    expect(posts).toHaveLength(0);
    await page.reload();
    await expectBalance(page, '0.00');
  });

  test('MT-026 Deposit field accepts whole dollars only', async ({ page }) => {
    await newCustomer(page, 'depfrac');
    const posts = trackPosts(page, '/deposit');
    await openPanel(page, 'depositForm');
    const amount = panel(page, 'depositForm').locator('input[name="amount"]');
    await amount.fill('10.50');
    await panel(page, 'depositForm').getByRole('button', { name: 'Submit' }).click();
    expect(await amount.evaluate((el: HTMLInputElement) => el.validity.stepMismatch)).toBe(true);
    expect(posts).toHaveLength(0);
    await page.reload();
    await expectBalance(page, '0.00');
  });

  test('MT-027 Large deposit is handled', async ({ page }) => {
    await newCustomer(page, 'depbig');
    await deposit(page, '1000000');
    await expectBalance(page, '1000000.00');
  });

  test('MT-028 Letters cannot be typed into amount', async ({ page }) => {
    await newCustomer(page, 'depabc');
    await openPanel(page, 'depositForm');
    const amount = panel(page, 'depositForm').locator('input[name="amount"]');
    await amount.click();
    await amount.pressSequentially('abc');
    await expect(amount).toHaveValue('');
  });
});
