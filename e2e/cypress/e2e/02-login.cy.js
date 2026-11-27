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
      cy.contains("form[action='/logout'] button", "Logout").should("be.visible");
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

  it("logs out through the dashboard Logout form", () => {
    cy.login(user, password);
    cy.visit("/dashboard");
    cy.contains("form[action='/logout'] button", "Logout").click();

    cy.location("pathname").should("eq", "/login");
    cy.location("search").should("eq", "?logout");
    cy.visit("/dashboard");
    cy.location("pathname").should("eq", "/login");
  });

  it("rejects a logout POST without a CSRF token and keeps the session", () => {
    cy.login(user, password);
    cy.request({ method: "POST", url: "/logout", failOnStatusCode: false, followRedirect: false })
      .its("status")
      .should("eq", 403);
    cy.visit("/dashboard");
    cy.location("pathname").should("eq", "/dashboard");
  });

  it("redirects anonymous visitors from the dashboard to the login page", () => {
    cy.visit("/dashboard");
    cy.location("pathname").should("eq", "/login");
  });
});
