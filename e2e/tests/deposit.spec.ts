import { test, expect } from './fixtures';

test.describe('Deposit', () => {
  test('MT-022 deposit increases balance', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.expectBalance('100.00');
  });

  test('MT-023 deposits add up', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.expectBalance('100.00');
    await loggedIn.deposit('250');
    await loggedIn.expectBalance('350.00');
  });

  test('MT-024 deposit appears in history', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.expectBalance('100.00');
    await loggedIn.openTransactions();
    await expect(loggedIn.historyAmount('Deposit')).toHaveText('100.00');
  });

  test('MT-025 empty deposit is blocked', async ({ loggedIn, page }) => {
    await loggedIn.submit('deposit', {});
    await expect(page).toHaveURL(/\/dashboard$/);
    await loggedIn.expectBalance('0.00');
  });

  test('MT-026 deposit field accepts whole dollars only', async ({ loggedIn, page }) => {
    await loggedIn.deposit('10.50');
    const amount = loggedIn.form('deposit').locator('input[name="amount"]');
    expect(await amount.evaluate((el: HTMLInputElement) => el.validity.stepMismatch)).toBe(true);
    await expect(page).toHaveURL(/\/dashboard$/);
    await loggedIn.expectBalance('0.00');
  });

  test('MT-027 large deposit is handled', async ({ loggedIn }) => {
    await loggedIn.deposit('1000000');
    await loggedIn.expectBalance('1000000.00');
  });

  test('MT-028 letters cannot be typed into amount', async ({ loggedIn }) => {
    await loggedIn.openPanel('deposit');
    const amount = loggedIn.form('deposit').locator('input[name="amount"]');
    await amount.pressSequentially('abc');
    await expect(amount).toHaveValue('');
  });
});
