import { test, expect } from '@playwright/test';
import { deposit, expectBalance, historyEntries, newCustomer, openPanel, panel, trackPosts, withdraw } from './helpers';

test.describe('Withdraw', () => {
  test('MT-029 Withdrawal decreases balance', async ({ page }) => {
    await newCustomer(page, 'wd');
    await deposit(page, '100');
    await withdraw(page, '40');
    await expect(page).toHaveURL(/\/dashboard$/);
    await expectBalance(page, '60.00');
  });

  test('MT-030 Customer can withdraw the full balance', async ({ page }) => {
    await newCustomer(page, 'wdall');
    await deposit(page, '100');
    await withdraw(page, '100');
    await expectBalance(page, '0.00');
  });

  test('MT-031 Overdraw is rejected', async ({ page }) => {
    await newCustomer(page, 'wdover');
    await deposit(page, '100');
    await withdraw(page, '500');
    await expect(page.getByText('Insufficient funds')).toBeVisible();
    await expectBalance(page, '100.00');
    await page.goto('/dashboard');
    await expectBalance(page, '100.00');
  });

  test('MT-032 Withdrawal from empty account is rejected', async ({ page }) => {
    await newCustomer(page, 'wdempty');
    await withdraw(page, '1');
    await expect(page.getByText('Insufficient funds')).toBeVisible();
    await page.goto('/dashboard');
    await expectBalance(page, '0.00');
  });

  test('MT-033 Withdrawal appears in history', async ({ page }) => {
    await newCustomer(page, 'wdhist');
    await deposit(page, '100');
    await withdraw(page, '40');
    await expectBalance(page, '60.00');
    const rows = await historyEntries(page);
    expect(rows).toContainEqual(expect.objectContaining({ type: 'Withdrawal', amount: '40.00' }));
  });

  test('MT-034 Empty withdrawal is blocked', async ({ page }) => {
    await newCustomer(page, 'wdblank');
    const posts = trackPosts(page, '/withdraw');
    await openPanel(page, 'withdrawForm');
    const amount = panel(page, 'withdrawForm').locator('input[name="amount"]');
    await panel(page, 'withdrawForm').getByRole('button', { name: 'Submit' }).click();
    expect(await amount.evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    expect(posts).toHaveLength(0);
    await expect(page).toHaveURL(/\/dashboard$/);
  });

  test('MT-035 Rejected withdrawal leaves no history', async ({ page }) => {
    await newCustomer(page, 'wdnohist');
    await deposit(page, '100');
    await withdraw(page, '500');
    await expect(page.getByText('Insufficient funds')).toBeVisible();
    const rows = await historyEntries(page);
    expect(rows.map((r) => r.type)).not.toContain('Withdrawal');
  });
});
