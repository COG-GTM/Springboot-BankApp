import { test, expect, deposit, errorMessage, expectBalance, history, newCustomer, openPanel, trackPosts, withdraw } from './fixtures';

test.describe('Withdraw', () => {
  test('MT-029 Withdrawal decreases balance', async ({ page }) => {
    await newCustomer(page);
    await deposit(page, '100');
    await withdraw(page, '40');
    await expectBalance(page, '60.00');
  });

  test('MT-030 Customer can withdraw the full balance', async ({ page }) => {
    await newCustomer(page);
    await deposit(page, '100');
    await withdraw(page, '100');
    await expect(errorMessage(page)).toHaveCount(0);
    await expectBalance(page, '0.00');
  });

  test('MT-031 Overdraw is rejected', async ({ page }) => {
    await newCustomer(page);
    await deposit(page, '100');
    await withdraw(page, '500');
    await expect(errorMessage(page)).toHaveText('Insufficient funds');
    await expectBalance(page, '100.00');
    await page.goto('/dashboard');
    await expectBalance(page, '100.00');
  });

  test('MT-032 Withdrawal from empty account is rejected', async ({ page }) => {
    await newCustomer(page);
    await withdraw(page, '1');
    await expect(errorMessage(page)).toHaveText('Insufficient funds');
    await page.goto('/dashboard');
    await expectBalance(page, '0.00');
  });

  test('MT-033 Withdrawal appears in history', async ({ page }) => {
    await newCustomer(page);
    await deposit(page, '100');
    await withdraw(page, '40');
    const rows = await history(page);
    expect(rows.filter((r) => r.type === 'Withdrawal').map((r) => r.amount)).toEqual(['40.00']);
  });

  test('MT-034 Empty withdrawal is blocked', async ({ page }) => {
    await newCustomer(page);
    await deposit(page, '100');
    const posts = trackPosts(page, '/withdraw');
    const form = await openPanel(page, 'withdraw');
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await form.locator('input[name="amount"]').evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    await expect(page).toHaveURL(/\/dashboard$/);
    expect(posts).toEqual([]);
  });

  test('MT-035 Rejected withdrawal leaves no history', async ({ page }) => {
    await newCustomer(page);
    await deposit(page, '100');
    await withdraw(page, '500');
    await expect(errorMessage(page)).toHaveText('Insufficient funds');
    const rows = await history(page);
    expect(rows.filter((r) => r.type === 'Withdrawal')).toEqual([]);
  });
});
