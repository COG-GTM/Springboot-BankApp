describe("Deposits and withdrawals", () => {
  const transactionRows = ($rows) => [...$rows]
    .map((row) => [...row.cells].map((cell) => cell.innerText.trim()))
    .sort((a, b) => Number(a[0]) - Number(b[0]));

  const resetDatabase = () => cy.exec("npm run stack:seed");

  beforeEach(() => {
    resetDatabase();
    cy.login(Cypress.env("seededUser"), Cypress.env("seededPassword"));
    cy.visit("/transactions");
    cy.get("tbody tr").should("have.length", 3)
      .then(transactionRows).as("transactionsBefore");
    cy.visit("/dashboard");
    cy.readBalance().should("eq", 500);
  });

  after(() => {
    resetDatabase();
  });

  function submitAmount(button, form, amount) {
    cy.contains("button", button).click();
    cy.get(form).should("be.visible").within(() => {
      cy.get('input[name="amount"]').type(String(amount));
      cy.get('button[type="submit"]').click();
    });
  }

  function expectTransaction(type, amount, color) {
    cy.contains("nav a", "Transactions").click();
    cy.location("pathname").should("eq", "/transactions");
    cy.get("@transactionsBefore").then((before) => {
      cy.get("tbody tr").should(($rows) => {
        const rows = transactionRows($rows);
        expect(rows).to.have.length(before.length + 1);
        expect(rows).to.deep.include.members(before);

        const added = rows.filter((row) => !before.some((old) => old[0] === row[0]));
        expect(added).to.have.length(1);
        expect(added[0][1]).to.equal(type);
        expect(added[0][2]).to.equal(amount);
        expect(added[0][3]).not.to.equal("");

        const addedRow = [...$rows].find((row) => row.cells[0].innerText.trim() === added[0][0]);
        expect(addedRow.cells[2]).to.have.class(color);
      });
    });
  }

  it("deposits money, persists the increased balance and adds one Deposit entry", () => {
    submitAmount("Deposit", "#depositForm", 125);

    cy.location("pathname").should("eq", "/dashboard");
    cy.get(".error").should("not.exist");
    cy.readBalance().should("eq", 625);
    cy.reload();
    cy.readBalance().should("eq", 625);
    expectTransaction("Deposit", "125.00", "text-success");
  });

  it("withdraws money, persists the decreased balance and adds one Withdrawal entry", () => {
    submitAmount("Withdraw", "#withdrawForm", 75);

    cy.location("pathname").should("eq", "/dashboard");
    cy.get(".error").should("not.exist");
    cy.readBalance().should("eq", 425);
    cy.reload();
    cy.readBalance().should("eq", 425);
    expectTransaction("Withdrawal", "75.00", "text-danger");
  });

  it("rejects an insufficient-funds withdrawal without changing balance or history", () => {
    submitAmount("Withdraw", "#withdrawForm", 501);

    cy.get(".error").should("be.visible").and("have.text", "Insufficient funds");
    cy.readBalance().should("eq", 500);
    cy.visit("/dashboard");
    cy.readBalance().should("eq", 500);
    cy.contains("nav a", "Transactions").click();
    cy.location("pathname").should("eq", "/transactions");
    cy.get("@transactionsBefore").then((before) => {
      cy.get("tbody tr").should(($rows) => {
        expect(transactionRows($rows)).to.deep.equal(before);
      });
    });
  });
});
