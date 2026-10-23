import { expect, test } from '@playwright/test';
import { deposit, expectBalance, historyTypesAndAmounts, newCustomer, openPanel, openTransactions, recordPosts } from './helpers';

test.describe('Deposit', () => {
  test('MT-022 Deposit increases balance', async ({ page, request }) => {
    await newCustomer(page, request, 'mt022');
    await deposit(page, '100');
    await expectBalance(page, '100.00');
  });

  test('MT-023 Deposits add up', async ({ page, request }) => {
    await newCustomer(page, request, 'mt023');
    await deposit(page, '100');
    await expectBalance(page, '100.00');
    await deposit(page, '250');
    await expectBalance(page, '350.00');
  });

  test('MT-024 Deposit appears in history', async ({ page, request }) => {
    await newCustomer(page, request, 'mt024');
    await deposit(page, '100');
    await openTransactions(page);
    expect(await historyTypesAndAmounts(page)).toEqual([{ type: 'Deposit', amount: '100.00' }]);
  });

  test('MT-025 Empty deposit is blocked', async ({ page, request }) => {
    await newCustomer(page, request, 'mt025');
    const posts = recordPosts(page, '/deposit');
    const form = await openPanel(page, 'deposit');
    await form.getByRole('button', { name: 'Submit' }).click();
    const amount = form.locator('input[name="amount"]');
    expect(await amount.evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    expect(posts).toEqual([]);
    await page.reload();
    await expectBalance(page, '0.00');
  });

  test('MT-026 Deposit field accepts whole dollars only', async ({ page, request }) => {
    await newCustomer(page, request, 'mt026');
    const posts = recordPosts(page, '/deposit');
    const form = await openPanel(page, 'deposit');
    const amount = form.locator('input[name="amount"]');
    await amount.fill('10.50');
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await amount.evaluate((el: HTMLInputElement) => el.validity.stepMismatch)).toBe(true);
    expect(posts).toEqual([]);
    await page.reload();
    await expectBalance(page, '0.00');
  });

  test('MT-027 Large deposit is handled', async ({ page, request }) => {
    await newCustomer(page, request, 'mt027');
    await deposit(page, '1000000');
    await expectBalance(page, '1000000.00');
  });

  test('MT-028 Letters cannot be typed into amount', async ({ page, request }) => {
    await newCustomer(page, request, 'mt028');
    const form = await openPanel(page, 'deposit');
    const amount = form.locator('input[name="amount"]');
    await amount.click();
    await page.keyboard.type('abc');
    await expect(amount).toHaveValue('');
  });
});
