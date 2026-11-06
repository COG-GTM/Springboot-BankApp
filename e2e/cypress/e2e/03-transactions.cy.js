describe("Transactions", () => {
  beforeEach(() => {
    cy.login(Cypress.env("seededUser"), Cypress.env("seededPassword"));
  });

  it("lists the seeded transactions for the signed-in account", () => {
    cy.visit("/transactions");
    cy.contains("h2", "Transaction History").should("be.visible");

    cy.get("table thead th").then(($th) => {
      const headers = [...$th].map((th) => th.innerText.trim());
      expect(headers).to.deep.equal(["ID", "Type", "Amount", "Date"]);
    });

    // alice's seed rows (see e2e/seed/seed.sql); bob's rows must not leak in.
    cy.get("table tbody tr").should("have.length", 3);
    cy.contains("tbody tr", "Deposit").within(() => {
      cy.get("td").eq(2).should("contain", "1000.00").and("have.class", "text-success");
    });
    cy.contains("tbody tr", "Withdrawal").within(() => {
      cy.get("td").eq(2).should("contain", "300.00").and("have.class", "text-danger");
    });
    cy.contains("tbody tr", "Transfer Out to bob").within(() => {
      cy.get("td").eq(2).should("contain", "200.00").and("have.class", "text-danger");
    });
    cy.contains("tbody tr", "Transfer In from alice").should("not.exist");

    cy.contains("a", "Back to Dashboard").should("have.attr", "href", "/dashboard");
  });
});
