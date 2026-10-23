import { test, expect } from './fixtures';

test.describe('Withdraw', () => {
  test('MT-029 withdrawal decreases balance', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.withdraw('40');
    await loggedIn.expectBalance('60.00');
  });

  test('MT-030 customer can withdraw the full balance', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.withdraw('100');
    await loggedIn.expectBalance('0.00');
  });

  test('MT-031 overdraw is rejected', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.withdraw('500');
    await expect(loggedIn.error).toHaveText('Insufficient funds');
    await loggedIn.expectBalance('100.00');
  });

  test('MT-032 withdrawal from empty account is rejected', async ({ loggedIn }) => {
    await loggedIn.withdraw('1');
    await expect(loggedIn.error).toHaveText('Insufficient funds');
    await loggedIn.expectBalance('0.00');
  });

  test('MT-033 withdrawal appears in history', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.withdraw('40');
    await loggedIn.expectBalance('60.00');
    await loggedIn.openTransactions();
    await expect(loggedIn.historyAmount('Withdrawal')).toHaveText('40.00');
  });

  test('MT-034 empty withdrawal is blocked', async ({ loggedIn, page }) => {
    await loggedIn.submit('withdraw', {});
    await expect(page).toHaveURL(/\/dashboard$/);
    await loggedIn.expectBalance('0.00');
  });

  test('MT-035 rejected withdrawal leaves no history', async ({ loggedIn }) => {
    await loggedIn.deposit('100');
    await loggedIn.withdraw('500');
    await expect(loggedIn.error).toHaveText('Insufficient funds');
    await loggedIn.openTransactions();
    await expect(loggedIn.historyRows()).toHaveCount(1);
    await expect(loggedIn.historyRow('Withdrawal')).toHaveCount(0);
  });
});
