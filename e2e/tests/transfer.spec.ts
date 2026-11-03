import { test, expect, registerViaApi, uniqueUser } from './fixtures';

test.describe('Transfer', () => {
  let recipient: string;

  test.beforeEach(async ({ request }) => {
    recipient = uniqueUser('payee');
    await registerViaApi(request, recipient);
  });

  test('MT-036 transfer debits the sender', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.transfer(recipient, '30');
    await loggedIn.expectBalance('70.00');
  });

  test('MT-037 transfer credits the recipient', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.transfer(recipient, '30');
    await loggedIn.expectBalance('70.00');
    await loggedIn.logout();
    await loggedIn.login(recipient);
    await loggedIn.expectBalance('30.00');
  });

  test('MT-038 sender history shows the transfer', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.transfer(recipient, '30');
    await loggedIn.expectBalance('70.00');
    await loggedIn.openTransactions();
    await expect(loggedIn.historyAmount(`Transfer Out to ${recipient}`)).toHaveText('30.00');
  });

  test('MT-039 recipient history shows the transfer', async ({ loggedIn, customer }) => {
    await loggedIn.deposit('100');
    await loggedIn.transfer(recipient, '30');
    await loggedIn.expectBalance('70.00');
    await loggedIn.logout();
    await loggedIn.login(recipient);
    await loggedIn.openTransactions();
    await expect(loggedIn.historyAmount(`Transfer In from ${customer}`)).toHaveText('30.00');
  });

  test('MT-040 customer can transfer the full balance', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.transfer(recipient, '100');
    await expect(loggedIn.error).toHaveCount(0);
    await loggedIn.expectBalance('0.00');
    await loggedIn.logout();
    await loggedIn.login(recipient);
    await loggedIn.expectBalance('100.00');
  });

  test('MT-041 transfer above balance is rejected', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.transfer(recipient, '150');
    await expect(loggedIn.error).toHaveText('Insufficient funds');
    await loggedIn.expectBalance('100.00');
    await loggedIn.logout();
    await loggedIn.login(recipient);
    await loggedIn.expectBalance('0.00');
  });

  test('MT-042 transfer to unknown customer is rejected', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.transfer(uniqueUser('nobody'), '10');
    await expect(loggedIn.error).toHaveText('Recipient account not found');
    await loggedIn.expectBalance('100.00');
  });

  test('MT-043 recipient is required', async ({ loggedIn, page }) => {
    await loggedIn.deposit('100');
    await loggedIn.submit('transfer', { amount: '10' });
    await expect(page).toHaveURL(/\/dashboard$/);
    await loggedIn.expectBalance('100.00');
  });

  test('MT-044 amount is required', async ({ loggedIn, page }) => {
    await loggedIn.deposit('100');
    await loggedIn.submit('transfer', { toUsername: recipient });
    await expect(page).toHaveURL(/\/dashboard$/);
    await loggedIn.expectBalance('100.00');
  });

  test('MT-045 multiple transfers add up', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.transfer(recipient, '20');
    await loggedIn.expectBalance('80.00');
    await loggedIn.transfer(recipient, '30');
    await loggedIn.expectBalance('50.00');
    await loggedIn.logout();
    await loggedIn.login(recipient);
    await loggedIn.expectBalance('50.00');
  });

  test('MT-046 rejected transfer leaves no history', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.transfer(uniqueUser('nobody'), '10');
    await expect(loggedIn.error).toHaveText('Recipient account not found');
    await loggedIn.openTransactions();
    await expect(loggedIn.historyRows()).toHaveCount(1);
    await expect(loggedIn.historyRow('Transfer')).toHaveCount(0);
  });
});
