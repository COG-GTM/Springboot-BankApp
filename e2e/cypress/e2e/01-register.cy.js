describe("Register", () => {
  it("renders the registration page", () => {
    cy.visit("/register");
    cy.contains("h2", "Register a New Account").should("be.visible");
    cy.get('form[action="/register"]').within(() => {
      cy.get('input[name="username"]').should("be.visible");
      cy.get('input[name="password"]').should("be.visible");
      cy.get('button[type="submit"]').should("contain", "Register");
    });
    cy.contains("a", "Login here").should("have.attr", "href", "/login");
  });

  it("creates a user who can then sign in", () => {
    const username = `cy_user_${Date.now()}`;
    const password = "Secret123!";

    cy.visit("/register");
    cy.get('input[name="username"]').type(username);
    cy.get('input[name="password"]').type(password, { log: false });
    cy.get('button[type="submit"]').click();

    cy.location("pathname").should("eq", "/login");

    cy.get('input[name="username"]').type(username);
    cy.get('input[name="password"]').type(password, { log: false });
    cy.get('button[type="submit"]').click();

    cy.location("pathname").should("eq", "/dashboard");
    cy.contains("h2", `Welcome, ${username}`).should("be.visible");
    cy.contains("h3", "Current Balance: $0").should("be.visible");
  });

  it("rejects a username that already exists", () => {
    cy.visit("/register");
    cy.get('input[name="username"]').type(Cypress.env("seededUser"));
    cy.get('input[name="password"]').type("whatever", { log: false });
    cy.get('button[type="submit"]').click();

    cy.location("pathname").should("eq", "/register");
    cy.get(".alert-danger").should("contain", "User already present");
  });
});
