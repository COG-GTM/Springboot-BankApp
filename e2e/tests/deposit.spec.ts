import { test, expect, deposit, expectBalance, history, newCustomer, openPanel, trackPosts } from './fixtures';

test.describe('Deposit', () => {
  test('MT-022 Deposit increases balance', async ({ page }) => {
    await newCustomer(page);
    await deposit(page, '100');
    await expectBalance(page, '100.00');
  });

  test('MT-023 Deposits add up', async ({ page }) => {
    await newCustomer(page);
    await deposit(page, '100');
    await deposit(page, '250');
    await expectBalance(page, '350.00');
  });

  test('MT-024 Deposit appears in history', async ({ page }) => {
    await newCustomer(page);
    await deposit(page, '100');
    const rows = await history(page);
    expect(rows.map(({ type, amount }) => ({ type, amount }))).toEqual([{ type: 'Deposit', amount: '100.00' }]);
  });

  test('MT-025 Empty deposit is blocked', async ({ page }) => {
    await newCustomer(page);
    const posts = trackPosts(page, '/deposit');
    const form = await openPanel(page, 'deposit');
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await form.locator('input[name="amount"]').evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    await expect(page).toHaveURL(/\/dashboard$/);
    expect(posts).toEqual([]);
    await page.reload();
    await expectBalance(page, '0.00');
  });

  test('MT-026 Deposit field accepts whole dollars only', async ({ page }) => {
    await newCustomer(page);
    const posts = trackPosts(page, '/deposit');
    const form = await openPanel(page, 'deposit');
    const amount = form.locator('input[name="amount"]');
    await amount.fill('10.50');
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await amount.evaluate((el: HTMLInputElement) => el.validity.stepMismatch)).toBe(true);
    expect(posts).toEqual([]);
    await page.reload();
    await expectBalance(page, '0.00');
  });

  test('MT-027 Large deposit is handled', async ({ page }) => {
    await newCustomer(page);
    await deposit(page, '1000000');
    await expectBalance(page, '1000000.00');
  });

  test('MT-028 Letters cannot be typed into amount', async ({ page }) => {
    await newCustomer(page);
    const form = await openPanel(page, 'deposit');
    const amount = form.locator('input[name="amount"]');
    await amount.pressSequentially('abc');
    await expect(amount).toHaveValue('');
  });
});
