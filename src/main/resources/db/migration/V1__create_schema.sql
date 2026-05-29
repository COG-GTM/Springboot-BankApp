CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    balance NUMERIC(19, 2) DEFAULT 0.00
);

CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(19, 2) NOT NULL,
    type VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    account_id BIGINT REFERENCES account(id)
);

CREATE INDEX idx_transaction_account_id ON transaction(account_id);
