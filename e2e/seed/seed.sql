-- Deterministic seed data for the Cypress smoke suite.
-- Applied after the application has started (Hibernate creates the schema
-- on boot with spring.jpa.hibernate.ddl-auto=update), so this file only
-- touches rows, never tables. Safe to re-run: it wipes and recreates the
-- fixture accounts and their transactions.
--
-- Every seeded account uses the password  Password123!
-- (bcrypt, cost 10 - the same encoder SecurityConfig uses).

SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM transaction;
DELETE FROM account;
ALTER TABLE transaction AUTO_INCREMENT = 1;
ALTER TABLE account AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO account (id, username, password, balance) VALUES
  (1, 'alice', '$2b$10$N2I.J1BcpYjpXRyHZA.HROyIvF5lClCEPMp3EQa/AtL3f8N9E3jEy', 500.00),
  (2, 'bob',   '$2b$10$N2I.J1BcpYjpXRyHZA.HROyIvF5lClCEPMp3EQa/AtL3f8N9E3jEy', 100.00);

INSERT INTO transaction (id, amount, type, timestamp, account_id) VALUES
  (1, 1000.00, 'Deposit',               '2024-01-05 09:00:00', 1),
  (2,  300.00, 'Withdrawal',            '2024-01-06 10:30:00', 1),
  (3,  200.00, 'Transfer Out to bob',   '2024-01-07 11:15:00', 1),
  (4,  200.00, 'Transfer In from alice','2024-01-07 11:15:00', 2),
  (5,  100.00, 'Withdrawal',            '2024-01-08 14:45:00', 2);
