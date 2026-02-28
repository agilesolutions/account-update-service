-- V1__create_accounts_table.sql
CREATE SEQUENCE IF NOT EXISTS account_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE accounts (
    id                      BIGSERIAL PRIMARY KEY,
    account_id              VARCHAR(11) NOT NULL UNIQUE,
    account_name            VARCHAR(25),
    account_type            VARCHAR(1) NOT NULL,
    active_status           CHAR(1) NOT NULL DEFAULT 'Y',
    curr_bal                NUMERIC(10,2) DEFAULT 0.00,
    credit_limit            NUMERIC(10,2) DEFAULT 0.00,
    cash_credit_limit       NUMERIC(10,2) DEFAULT 0.00,
    open_date               DATE,
    expiry_date             DATE,
    reissue_date            DATE,
    curr_cycle_credit        NUMERIC(10,2) DEFAULT 0.00,
    curr_cycle_debit         NUMERIC(10,2) DEFAULT 0.00,
    addr_zip                VARCHAR(10),
    addr_state              VARCHAR(20),
    addr_country            VARCHAR(20),
    addr_line1              VARCHAR(50),
    addr_line2              VARCHAR(50),
    phone_number_1          VARCHAR(15),
    phone_number_2          VARCHAR(15),
    group_id                VARCHAR(10),
    student_ind             CHAR(1) DEFAULT 'N',
    over_limit_ind          CHAR(1) DEFAULT 'N',
    created_by              VARCHAR(50),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by              VARCHAR(50),
    updated_at              TIMESTAMP,
    version                 BIGINT DEFAULT 0,
    CONSTRAINT chk_active_status CHECK (active_status IN ('Y','N')),
    CONSTRAINT chk_account_type CHECK (account_type IN ('1','2'))
);

CREATE INDEX idx_account_id ON accounts(account_id);
CREATE INDEX idx_account_type ON accounts(account_type);
CREATE INDEX idx_active_status ON accounts(active_status);