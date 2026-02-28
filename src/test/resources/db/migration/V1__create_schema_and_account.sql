-- ============================================================
-- Flyway migration V1: Create carddemo schema and account table
--
-- COBOL source : CVACT01Y copybook / FD-ACCTFILE-REC
-- VSAM file    : ACCTFILE  (KSDS, PK = FD-ACCT-ID PIC 9(11))
--
-- Column widths match COBOL PIC clauses exactly.
-- Monetary amounts use NUMERIC(12,2) — generous enough for
-- S9(10)V99 COMP-3 packed decimal.
-- ============================================================

CREATE SCHEMA IF NOT EXISTS carddemo;

-- -------------------------------------------------------
-- account
-- -------------------------------------------------------
CREATE TABLE carddemo.account (
    -- ACCT-ID  PIC 9(11)  — zero-padded string to preserve leading zeros
    acct_id                 CHAR(11)        NOT NULL,

    -- ACCT-ACTIVE-STATUS  PIC X(01)
    acct_active_status      CHAR(1),

    -- ACCT-CURR-BAL  PIC S9(10)V99 COMP-3
    acct_curr_bal           NUMERIC(12, 2)  NOT NULL DEFAULT 0,

    -- ACCT-CREDIT-LIMIT  PIC S9(10)V99 COMP-3
    acct_credit_limit       NUMERIC(12, 2),

    -- ACCT-CASH-CREDIT-LIMIT  PIC S9(10)V99 COMP-3
    acct_cash_credit_limit  NUMERIC(12, 2),

    -- ACCT-OPEN-DATE  PIC X(10)
    acct_open_date          CHAR(10),

    -- ACCT-EXPIRAION-DATE  PIC X(10)
    acct_expiration_date    CHAR(10),

    -- ACCT-REISSUE-DATE  PIC X(10)
    acct_reissue_date       CHAR(10),

    -- ACCT-CURR-CYC-CREDIT  PIC S9(10)V99 — zeroed after each interest run
    acct_curr_cyc_credit    NUMERIC(12, 2)  NOT NULL DEFAULT 0,

    -- ACCT-CURR-CYC-DEBIT   PIC S9(10)V99 — zeroed after each interest run
    acct_curr_cyc_debit     NUMERIC(12, 2)  NOT NULL DEFAULT 0,

    -- ACCT-ADDR-ZIP  PIC X(10)
    acct_addr_zip           VARCHAR(10),

    -- ACCT-GROUP-ID  PIC X(10) — links to disclosure_group
    acct_group_id           CHAR(10),

    -- Audit columns (not in COBOL, added for PostgreSQL)
    created_at              TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT pk_account PRIMARY KEY (acct_id)
);

COMMENT ON TABLE  carddemo.account                    IS 'Credit card accounts. COBOL: CVACT01Y / ACCTFILE VSAM.';
COMMENT ON COLUMN carddemo.account.acct_id            IS 'ACCT-ID PIC 9(11) — VSAM primary key';
COMMENT ON COLUMN carddemo.account.acct_curr_bal      IS 'Running balance; CBACT04C adds monthly interest here';
COMMENT ON COLUMN carddemo.account.acct_curr_cyc_credit IS 'Reset to 0 by CBACT04C after each interest run';
COMMENT ON COLUMN carddemo.account.acct_curr_cyc_debit  IS 'Reset to 0 by CBACT04C after each interest run';
COMMENT ON COLUMN carddemo.account.acct_group_id      IS 'Links to disclosure_group.acct_group_id for rate lookup';
