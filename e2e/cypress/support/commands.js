/**
 * Log in through the real login form so the browser holds the JSESSIONID
 * cookie the way a user would. Wrapped in cy.session so that specs that need
 * an authenticated user do not pay for the form round-trip on every test.
 */
Cypress.Commands.add("login", (username, password) => {
  cy.session(
    [username, password],
    () => {
      cy.visit("/login");
      cy.get('input[name="username"]').type(username);
      cy.get('input[name="password"]').type(password, { log: false });
      cy.get('button[type="submit"]').click();
      cy.url().should("include", "/dashboard");
    },
    {
      validate() {
        cy.request({ url: "/dashboard", followRedirect: false })
          .its("status")
          .should("eq", 200);
      },
    }
  );
});

/** Read "Current Balance: $123.45" from the dashboard and yield 123.45. */
Cypress.Commands.add("readBalance", () => {
  cy.contains("h3", "Current Balance:")
    .invoke("text")
    .then((text) => Number(text.replace(/[^0-9.]/g, "")));
});
