import { expect, test } from '@playwright/test';
import { deposit, expectBalance, historyTypesAndAmounts, newCustomer, openPanel, openTransactions, recordPosts, withdraw } from './helpers';

test.describe('Withdraw', () => {
  test('MT-029 Withdrawal decreases balance', async ({ page, request }) => {
    await newCustomer(page, request, 'mt029');
    await deposit(page, '100');
    await withdraw(page, '40');
    await expectBalance(page, '60.00');
  });

  test('MT-030 Customer can withdraw the full balance', async ({ page, request }) => {
    await newCustomer(page, request, 'mt030');
    await deposit(page, '100');
    await withdraw(page, '100');
    await expectBalance(page, '0.00');
  });

  test('MT-031 Overdraw is rejected', async ({ page, request }) => {
    await newCustomer(page, request, 'mt031');
    await deposit(page, '100');
    await withdraw(page, '500');
    await expect(page.locator('p.error')).toHaveText('Insufficient funds');
    await expectBalance(page, '100.00');
    await page.goto('/dashboard');
    await expectBalance(page, '100.00');
  });

  test('MT-032 Withdrawal from empty account is rejected', async ({ page, request }) => {
    await newCustomer(page, request, 'mt032');
    await withdraw(page, '1');
    await expect(page.locator('p.error')).toHaveText('Insufficient funds');
    await expectBalance(page, '0.00');
  });

  test('MT-033 Withdrawal appears in history', async ({ page, request }) => {
    await newCustomer(page, request, 'mt033');
    await deposit(page, '100');
    await withdraw(page, '40');
    await openTransactions(page);
    expect(await historyTypesAndAmounts(page)).toContainEqual({ type: 'Withdrawal', amount: '40.00' });
  });

  test('MT-034 Empty withdrawal is blocked', async ({ page, request }) => {
    await newCustomer(page, request, 'mt034');
    const posts = recordPosts(page, '/withdraw');
    const form = await openPanel(page, 'withdraw');
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await form.locator('input[name="amount"]').evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    expect(posts).toEqual([]);
    await expect(page).toHaveURL(/\/dashboard$/);
  });

  test('MT-035 Rejected withdrawal leaves no history', async ({ page, request }) => {
    await newCustomer(page, request, 'mt035');
    await deposit(page, '100');
    await withdraw(page, '500');
    await expect(page.locator('p.error')).toHaveText('Insufficient funds');
    await openTransactions(page);
    const types = (await historyTypesAndAmounts(page)).map((r) => r.type);
    expect(types).toEqual(['Deposit']);
  });
});
