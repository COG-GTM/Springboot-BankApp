import { expect, test } from '@playwright/test';
import {
  deposit,
  expectBalance,
  historyTypesAndAmounts,
  newCustomer,
  openPanel,
  openTransactions,
  recordPosts,
  registerViaHttp,
  sessionFor,
  transfer,
  uniqueUsername,
} from './helpers';

test.describe('Transfer', () => {
  test('MT-036 Transfer debits the sender', async ({ page, request }) => {
    const recipient = uniqueUsername('mt036_to');
    await registerViaHttp(request, recipient);
    await newCustomer(page, request, 'mt036');
    await deposit(page, '100');
    await transfer(page, recipient, '30');
    await expectBalance(page, '70.00');
  });

  test('MT-037 Transfer credits the recipient', async ({ page, request, browser }) => {
    const recipient = uniqueUsername('mt037_to');
    await registerViaHttp(request, recipient);
    await newCustomer(page, request, 'mt037');
    await deposit(page, '100');
    await transfer(page, recipient, '30');
    await expectBalance(page, '70.00');

    const recipientPage = await sessionFor(browser, recipient);
    await expectBalance(recipientPage, '30.00');
    await recipientPage.context().close();
  });

  test('MT-038 Sender history shows the transfer', async ({ page, request }) => {
    const recipient = uniqueUsername('mt038_to');
    await registerViaHttp(request, recipient);
    await newCustomer(page, request, 'mt038');
    await deposit(page, '100');
    await transfer(page, recipient, '30');
    await openTransactions(page);
    expect(await historyTypesAndAmounts(page)).toContainEqual({ type: `Transfer Out to ${recipient}`, amount: '30.00' });
  });

  test('MT-039 Recipient history shows the transfer', async ({ page, request, browser }) => {
    const recipient = uniqueUsername('mt039_to');
    await registerViaHttp(request, recipient);
    const sender = await newCustomer(page, request, 'mt039');
    await deposit(page, '100');
    await transfer(page, recipient, '30');

    const recipientPage = await sessionFor(browser, recipient);
    await openTransactions(recipientPage);
    expect(await historyTypesAndAmounts(recipientPage)).toEqual([{ type: `Transfer In from ${sender}`, amount: '30.00' }]);
    await recipientPage.context().close();
  });

  test('MT-040 Customer can transfer the full balance', async ({ page, request, browser }) => {
    const recipient = uniqueUsername('mt040_to');
    await registerViaHttp(request, recipient);
    await newCustomer(page, request, 'mt040');
    await deposit(page, '100');
    await transfer(page, recipient, '100');
    await expectBalance(page, '0.00');

    const recipientPage = await sessionFor(browser, recipient);
    await expectBalance(recipientPage, '100.00');
    await recipientPage.context().close();
  });

  test('MT-041 Transfer above balance is rejected', async ({ page, request, browser }) => {
    const recipient = uniqueUsername('mt041_to');
    await registerViaHttp(request, recipient);
    await newCustomer(page, request, 'mt041');
    await deposit(page, '100');
    await transfer(page, recipient, '150');
    await expect(page.locator('p.error')).toHaveText('Insufficient funds');
    await page.goto('/dashboard');
    await expectBalance(page, '100.00');

    const recipientPage = await sessionFor(browser, recipient);
    await expectBalance(recipientPage, '0.00');
    await recipientPage.context().close();
  });

  // Precondition: the sender holds funds. The app checks the balance before the recipient,
  // so an empty account would see 'Insufficient funds' instead of the message under test.
  test('MT-042 Transfer to unknown customer is rejected', async ({ page, request }) => {
    await newCustomer(page, request, 'mt042');
    await deposit(page, '100');
    await transfer(page, uniqueUsername('mt042_missing'), '10');
    await expect(page.locator('p.error')).toHaveText('Recipient account not found');
    await page.goto('/dashboard');
    await expectBalance(page, '100.00');
  });

  test('MT-043 Recipient is required', async ({ page, request }) => {
    await newCustomer(page, request, 'mt043');
    const posts = recordPosts(page, '/transfer');
    const form = await openPanel(page, 'transfer');
    await form.locator('input[name="amount"]').fill('10');
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await form.locator('input[name="toUsername"]').evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    expect(posts).toEqual([]);
  });

  test('MT-044 Amount is required', async ({ page, request }) => {
    const recipient = uniqueUsername('mt044_to');
    await registerViaHttp(request, recipient);
    await newCustomer(page, request, 'mt044');
    const posts = recordPosts(page, '/transfer');
    const form = await openPanel(page, 'transfer');
    await form.locator('input[name="toUsername"]').fill(recipient);
    await form.getByRole('button', { name: 'Submit' }).click();
    expect(await form.locator('input[name="amount"]').evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true);
    expect(posts).toEqual([]);
  });

  test('MT-045 Multiple transfers add up', async ({ page, request, browser }) => {
    const recipient = uniqueUsername('mt045_to');
    await registerViaHttp(request, recipient);
    await newCustomer(page, request, 'mt045');
    await deposit(page, '100');
    await transfer(page, recipient, '20');
    await expectBalance(page, '80.00');
    await transfer(page, recipient, '30');
    await expectBalance(page, '50.00');

    const recipientPage = await sessionFor(browser, recipient);
    await expectBalance(recipientPage, '50.00');
    await recipientPage.context().close();
  });

  test('MT-046 Rejected transfer leaves no history', async ({ page, request }) => {
    await newCustomer(page, request, 'mt046');
    await deposit(page, '100');
    await transfer(page, uniqueUsername('mt046_missing'), '10');
    await expect(page.locator('p.error')).toHaveText('Recipient account not found');
    await openTransactions(page);
    const types = (await historyTypesAndAmounts(page)).map((r) => r.type);
    expect(types.filter((t) => t.includes('Transfer'))).toEqual([]);
    expect(types).toEqual(['Deposit']);
  });
});
