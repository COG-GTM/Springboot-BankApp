-- Stored-value (gift card) schema. Hibernate (spring.jpa.hibernate.ddl-auto=update) creates these
-- tables at startup; this script provisions the same schema for environments managed up front.

USE bankappdb;

CREATE TABLE IF NOT EXISTS stored_value_card (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    card_token     VARCHAR(64)   NOT NULL,
    card_reference VARCHAR(32)   NOT NULL,
    currency       VARCHAR(3)    NOT NULL,
    initial_amount DECIMAL(19,2) NOT NULL,
    balance        DECIMAL(19,2) NOT NULL,
    status         VARCHAR(16)   NOT NULL,
    expires_at     DATETIME(6)   NULL,
    issued_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_stored_value_card_token (card_token)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stored_value_transaction (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    card_id         BIGINT        NOT NULL,
    type            VARCHAR(16)   NOT NULL,
    amount          DECIMAL(19,2) NOT NULL,
    balance_after   DECIMAL(19,2) NOT NULL,
    idempotency_key VARCHAR(128)  NULL,
    created_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stored_value_txn_idempotency (card_id, idempotency_key),
    CONSTRAINT fk_stored_value_txn_card FOREIGN KEY (card_id) REFERENCES stored_value_card (id)
) ENGINE=InnoDB;
