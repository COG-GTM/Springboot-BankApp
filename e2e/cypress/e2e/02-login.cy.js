describe("Login", () => {
  const user = Cypress.env("seededUser");
  const password = Cypress.env("seededPassword");

  it("signs in a seeded user and lands on the dashboard", () => {
    cy.visit("/login");
    cy.get('input[name="username"]').type(user);
    cy.get('input[name="password"]').type(password, { log: false });
    cy.get('button[type="submit"]').click();

    cy.location("pathname").should("eq", "/dashboard");
    cy.contains("h2", `Welcome, ${user}`).should("be.visible");
    cy.contains("h3", "Current Balance: $500.00").should("be.visible");
    cy.contains(".account-details p", "Account Number:").should("be.visible");
    cy.get("nav").within(() => {
      cy.contains("a", "Dashboard").should("have.attr", "href", "/dashboard");
      cy.contains("a", "Transactions").should("have.attr", "href", "/transactions");
      cy.contains("a", "Logout").should("have.attr", "href", "/logout");
    });
  });

  it("stays on the login page with an error for a wrong password", () => {
    cy.visit("/login");
    cy.get('input[name="username"]').type(user);
    cy.get('input[name="password"]').type("not-the-password", { log: false });
    cy.get('button[type="submit"]').click();

    cy.location("pathname").should("eq", "/login");
    cy.location("search").should("eq", "?error");
  });

  it("redirects anonymous visitors from the dashboard to the login page", () => {
    cy.visit("/dashboard");
    cy.location("pathname").should("eq", "/login");
  });
});
