import { test, expect } from '@playwright/test';
import {
  deposit, errorMessage, expectBalance, historyRow, newCustomerSession, openHistory, openPanel, recordPosts, withdraw,
} from './helpers';

test.describe('Withdraw', () => {
  test('MT-029 Withdrawal decreases balance', async ({ page }) => {
    await newCustomerSession(page, 'mt029');
    await deposit(page, 100);
    await withdraw(page, 40);
    await expectBalance(page, '60.00');
  });

  test('MT-030 Customer can withdraw the full balance', async ({ page }) => {
    await newCustomerSession(page, 'mt030');
    await deposit(page, 100);
    await withdraw(page, 100);
    await expect(errorMessage(page)).toHaveCount(0);
    await expectBalance(page, '0.00');
  });

  test('MT-031 Overdraw is rejected', async ({ page }) => {
    await newCustomerSession(page, 'mt031');
    await deposit(page, 100);
    await withdraw(page, 500);
    await expect(errorMessage(page)).toHaveText('Insufficient funds');
    await expectBalance(page, '100.00');
    await page.goto('/dashboard');
    await expectBalance(page, '100.00');
  });

  test('MT-032 Withdrawal from empty account is rejected', async ({ page }) => {
    await newCustomerSession(page, 'mt032');
    await withdraw(page, 1);
    await expect(errorMessage(page)).toHaveText('Insufficient funds');
  });

  test('MT-033 Withdrawal appears in history', async ({ page }) => {
    await newCustomerSession(page, 'mt033');
    await deposit(page, 100);
    await withdraw(page, 40);
    await openHistory(page);
    const row = historyRow(page, 'Withdrawal');
    await expect(row).toHaveCount(1);
    await expect(row.locator('td').nth(2)).toContainText('40.00');
  });

  test('MT-034 Empty withdrawal is blocked', async ({ page }) => {
    await newCustomerSession(page, 'mt034');
    const posts = recordPosts(page);
    const form = await openPanel(page, 'Withdraw');
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await form.locator('input[name="amount"]').evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    await expect(page).toHaveURL(/\/dashboard$/);
    expect(posts).toEqual([]);
  });

  test('MT-035 Rejected withdrawal leaves no history', async ({ page }) => {
    await newCustomerSession(page, 'mt035');
    await deposit(page, 100);
    await withdraw(page, 500);
    await expect(errorMessage(page)).toHaveText('Insufficient funds');
    await openHistory(page);
    await expect(historyRow(page, 'Withdrawal')).toHaveCount(0);
    await expect(historyRow(page, 'Deposit')).toHaveCount(1);
  });
});
