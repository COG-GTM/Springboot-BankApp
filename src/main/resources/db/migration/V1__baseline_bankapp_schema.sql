-- Baseline schema for the bank application.
-- Mirrors the JPA entities com.example.bankapp.model.Account and
-- com.example.bankapp.model.Transaction as they existed when Flyway was
-- introduced, so `spring.jpa.hibernate.ddl-auto=validate` succeeds on a clean database.

CREATE TABLE IF NOT EXISTS account (
    id       BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255),
    password VARCHAR(255),
    balance  DECIMAL(38, 2),
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS transaction (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    amount     DECIMAL(38, 2),
    type       VARCHAR(255),
    timestamp  DATETIME(6),
    account_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES account (id)
) ENGINE = InnoDB;
