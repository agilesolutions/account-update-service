-- =====================================================
-- Credit Card Table Migration
-- Migrated from COBOL COCRDUPC.cbl VSAM/DB2 structures
-- CARD-RECORD copybook → credit_cards table
-- =====================================================

CREATE SEQUENCE card_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE credit_cards (
    -- Primary key (replaces VSAM RIDFLD)
    id                   BIGINT DEFAULT nextval('card_sequence') PRIMARY KEY,

    -- CDEMO-CARD-NUM PIC X(16)
    card_number          VARCHAR(16)     NOT NULL UNIQUE,

    -- CDEMO-CARD-ACCT-ID PIC 9(11)
    account_id           VARCHAR(11)     NOT NULL,

    -- CDEMO-CARD-CVV-CD PIC 9(3)
    cvv_code             VARCHAR(3)      NOT NULL,

    -- CDEMO-CARD-EMBOSSED-NAME PIC X(50)
    embossed_name        VARCHAR(50)     NOT NULL,

    -- CDEMO-CARD-EXPIRAION-DATE PIC X(10) → stored as DATE
    expiration_date      DATE            NOT NULL,

    -- CDEMO-CARD-ACTIVE-STATUS PIC X(1)
    active_status        CHAR(1)         NOT NULL DEFAULT 'Y',

    -- CDEMO-CARD-CREDIT-LIMIT PIC S9(10)V99
    credit_limit         DECIMAL(12, 2)  DEFAULT 0.00,

    -- CDEMO-CARD-CASH-CREDIT-LIMIT PIC S9(10)V99
    cash_credit_limit    DECIMAL(12, 2)  DEFAULT 0.00,

    -- CDEMO-CARD-CURR-BAL PIC S9(10)V99
    current_balance      DECIMAL(12, 2)  DEFAULT 0.00,

    -- CDEMO-CARD-CURR-CYC-CREDIT PIC S9(10)V99
    current_cycle_credit DECIMAL(12, 2)  DEFAULT 0.00,

    -- CDEMO-CARD-CURR-CYC-DEBIT PIC S9(10)V99
    current_cycle_debit  DECIMAL(12, 2)  DEFAULT 0.00,

    -- CDEMO-CARD-GROUP-ID PIC X(10)
    group_id             VARCHAR(10),

    -- Audit columns (replaces COBOL timestamp fields)
    created_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP,
    version              BIGINT          DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_active_status CHECK (active_status IN ('Y', 'N')),
    CONSTRAINT chk_credit_limit CHECK (credit_limit >= 0),
    CONSTRAINT chk_cash_credit_limit CHECK (cash_credit_limit >= 0),
    CONSTRAINT chk_cash_lte_credit CHECK (cash_credit_limit <= credit_limit)
);

-- Indexes (replaces VSAM alternate indexes)
CREATE INDEX idx_account_id ON credit_cards(account_id);
CREATE INDEX idx_active_status ON credit_cards(active_status);
CREATE INDEX idx_expiration_date ON credit_cards(expiration_date);

-- Comments for documentation
COMMENT ON TABLE credit_cards IS 'Credit card records - migrated from COBOL VSAM CARDDAT file';
COMMENT ON COLUMN credit_cards.card_number IS 'COBOL: CDEMO-CARD-NUM PIC X(16)';
COMMENT ON COLUMN credit_cards.account_id IS 'COBOL: CDEMO-CARD-ACCT-ID PIC 9(11)';
COMMENT ON COLUMN credit_cards.active_status IS 'COBOL: CDEMO-CARD-ACTIVE-STATUS PIC X(1) - Y=Active, N=Inactive';

-- =====================================================
-- V2: Sample test data (separate migration file)
-- =====================================================
INSERT INTO credit_cards (
    card_number, account_id, cvv_code, embossed_name,
    expiration_date, active_status, credit_limit, cash_credit_limit,
    current_balance, current_cycle_credit, current_cycle_debit, group_id
) VALUES
(
    '4111111111111111', '00000001001', '123', 'JOHN DOE',
    '2028-12-31', 'Y', 5000.00, 1500.00,
    1250.75, 500.00, 250.25, 'PREMIUM'
),
(
    '5500000000000004', '00000001002', '456', 'JANE SMITH',
    '2027-06-30', 'Y', 10000.00, 3000.00,
    3500.00, 1000.00, 750.00, 'GOLD'
),
(
    '4012888888881881', '00000001003', '789', 'BOB JOHNSON',
    '2026-09-30', 'N', 2500.00, 500.00,
    0.00, 0.00, 0.00, 'STANDARD'
);