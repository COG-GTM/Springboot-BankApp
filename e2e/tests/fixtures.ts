import { test as base } from '@playwright/test';
import { BankApp, PASSWORD, registerViaApi, uniqueUser } from '../pages/BankApp';

type Fixtures = {
  app: BankApp;
  customer: string;
  loggedIn: BankApp;
};

export const test = base.extend<Fixtures>({
  app: async ({ page }, use) => {
    await use(new BankApp(page));
  },
  customer: async ({ request }, use) => {
    const username = uniqueUser();
    await registerViaApi(request, username);
    await use(username);
  },
  loggedIn: async ({ app, customer }, use) => {
    await app.login(customer);
    await app.expectBalance('0.00');
    await use(app);
  },
});

export { expect } from '@playwright/test';
export { BankApp, PASSWORD, registerViaApi, uniqueUser };
