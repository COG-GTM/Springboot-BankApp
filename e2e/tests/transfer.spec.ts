import { test, expect } from '@playwright/test';
import {
  balanceOf,
  deposit,
  expectBalance,
  historyEntries,
  login,
  newCustomer,
  openPanel,
  panel,
  registerOther,
  trackPosts,
  transfer,
  uniqueUsername,
} from './helpers';

test.describe('Transfer', () => {
  test('MT-036 Transfer debits the sender', async ({ page, browser }) => {
    await newCustomer(page, 'snd');
    const recipient = await registerOther(browser);
    await deposit(page, '100');
    await transfer(page, recipient, '30');
    await expect(page).toHaveURL(/\/dashboard$/);
    await expectBalance(page, '70.00');
  });

  test('MT-037 Transfer credits the recipient', async ({ page, browser }) => {
    await newCustomer(page, 'snd');
    const recipient = await registerOther(browser);
    await deposit(page, '100');
    await transfer(page, recipient, '30');
    await expectBalance(page, '70.00');

    await page.getByRole('link', { name: 'Logout' }).click();
    await login(page, recipient);
    await expect(page).toHaveURL(/\/dashboard$/);
    await expectBalance(page, '30.00');
  });

  test('MT-038 Sender history shows the transfer', async ({ page, browser }) => {
    await newCustomer(page, 'snd');
    const recipient = await registerOther(browser);
    await deposit(page, '100');
    await transfer(page, recipient, '30');
    await expectBalance(page, '70.00');
    const rows = await historyEntries(page);
    expect(rows).toContainEqual(expect.objectContaining({ type: `Transfer Out to ${recipient}`, amount: '30.00' }));
  });

  test('MT-039 Recipient history shows the transfer', async ({ page, browser }) => {
    const sender = await newCustomer(page, 'snd');
    const recipient = await registerOther(browser);
    await deposit(page, '100');
    await transfer(page, recipient, '30');
    await expectBalance(page, '70.00');

    await page.getByRole('link', { name: 'Logout' }).click();
    await login(page, recipient);
    const rows = await historyEntries(page);
    expect(rows).toContainEqual(expect.objectContaining({ type: `Transfer In from ${sender}`, amount: '30.00' }));
  });

  test('MT-040 Customer can transfer the full balance', async ({ page, browser }) => {
    await newCustomer(page, 'snd');
    const recipient = await registerOther(browser);
    await deposit(page, '100');
    await transfer(page, recipient, '100');
    await expectBalance(page, '0.00');
    expect(await balanceOf(browser, recipient)).toBe('Current Balance: $100.00');
  });

  test('MT-041 Transfer above balance is rejected', async ({ page, browser }) => {
    await newCustomer(page, 'snd');
    const recipient = await registerOther(browser);
    await deposit(page, '100');
    await transfer(page, recipient, '150');
    await expect(page.getByText('Insufficient funds')).toBeVisible();
    await page.goto('/dashboard');
    await expectBalance(page, '100.00');
    expect(await balanceOf(browser, recipient)).toBe('Current Balance: $0.00');
  });

  // Plan does not state a starting balance; funds are deposited first so the
  // insufficient-funds check (which runs before the recipient lookup) cannot mask this case.
  test('MT-042 Transfer to unknown customer is rejected', async ({ page }) => {
    await newCustomer(page, 'snd');
    await deposit(page, '100');
    await transfer(page, uniqueUsername('nobody'), '10');
    await expect(page.getByText('Recipient account not found')).toBeVisible();
    await page.goto('/dashboard');
    await expectBalance(page, '100.00');
  });

  test('MT-043 Recipient is required', async ({ page }) => {
    await newCustomer(page, 'snd');
    const posts = trackPosts(page, '/transfer');
    await openPanel(page, 'transferForm');
    const form = panel(page, 'transferForm');
    await form.locator('input[name="amount"]').fill('10');
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await form.locator('input[name="toUsername"]').evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    expect(posts).toHaveLength(0);
  });

  test('MT-044 Amount is required', async ({ page, browser }) => {
    await newCustomer(page, 'snd');
    const recipient = await registerOther(browser);
    const posts = trackPosts(page, '/transfer');
    await openPanel(page, 'transferForm');
    const form = panel(page, 'transferForm');
    await form.locator('input[name="toUsername"]').fill(recipient);
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await form.locator('input[name="amount"]').evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    expect(posts).toHaveLength(0);
  });

  test('MT-045 Multiple transfers add up', async ({ page, browser }) => {
    await newCustomer(page, 'snd');
    const recipient = await registerOther(browser);
    await deposit(page, '100');
    await transfer(page, recipient, '20');
    await expectBalance(page, '80.00');
    await transfer(page, recipient, '30');
    await expectBalance(page, '50.00');
    expect(await balanceOf(browser, recipient)).toBe('Current Balance: $50.00');
  });

  test('MT-046 Rejected transfer leaves no history', async ({ page }) => {
    await newCustomer(page, 'snd');
    await deposit(page, '100');
    await transfer(page, uniqueUsername('nobody'), '10');
    await expect(page.getByText('Recipient account not found')).toBeVisible();
    const rows = await historyEntries(page);
    expect(rows.filter((r) => r.type.startsWith('Transfer'))).toEqual([]);
  });
});
