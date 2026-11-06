describe("Transfer", () => {
  const sender = Cypress.env("seededUser");
  const recipient = Cypress.env("seededRecipient");
  const password = Cypress.env("seededPassword");
  const amount = 125;

  beforeEach(() => {
    cy.login(sender, password);
  });

  it("submits the transfer form and shows the updated balance", () => {
    cy.visit("/dashboard");

    cy.readBalance().then((before) => {
      cy.contains("button", "Transfer Money").click();
      cy.get("#transferForm").should("be.visible").within(() => {
        cy.get('input[name="toUsername"]').type(recipient);
        cy.get('input[name="amount"]').type(String(amount));
        cy.get('button[type="submit"]').click();
      });

      cy.location("pathname").should("eq", "/dashboard");
      cy.get(".error").should("not.exist");
      cy.readBalance().should("eq", Number((before - amount).toFixed(2)));
    });

    cy.visit("/transactions");
    cy.get("tbody tr")
      .filter(`:contains("Transfer Out to ${recipient}")`)
      .last()
      .find("td")
      .eq(2)
      .should("contain", `${amount}.00`)
      .and("have.class", "text-danger");
  });

  it("shows an error and leaves the balance alone when funds are insufficient", () => {
    cy.visit("/dashboard");

    cy.readBalance().then((before) => {
      cy.contains("button", "Transfer Money").click();
      cy.get("#transferForm").should("be.visible").within(() => {
        cy.get('input[name="toUsername"]').type(recipient);
        cy.get('input[name="amount"]').type(String(before + 1000));
        cy.get('button[type="submit"]').click();
      });

      cy.get(".error").should("contain", "Insufficient funds");
      cy.readBalance().should("eq", before);
    });
  });

  it("shows an error when the recipient does not exist", () => {
    cy.visit("/dashboard");
    cy.contains("button", "Transfer Money").click();
    cy.get("#transferForm").should("be.visible").within(() => {
      cy.get('input[name="toUsername"]').type("nobody-here");
      cy.get('input[name="amount"]').type("1");
      cy.get('button[type="submit"]').click();
    });
    cy.get(".error").should("contain", "Recipient account not found");
  });
});
