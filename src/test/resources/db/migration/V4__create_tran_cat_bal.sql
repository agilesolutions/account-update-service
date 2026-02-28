-- ============================================================
-- Flyway migration V4: tran_cat_bal table
--
-- COBOL source : CVTRA01Y copybook / FD-TRAN-CAT-BAL-RECORD
-- VSAM file    : TCATBAL-FILE  (KSDS, sequential read in CBACT04C)
-- Composite PK : FD-TRANCAT-ACCT-ID   PIC 9(11)
--              + FD-TRANCAT-TYPE-CD   PIC X(02)
--              + FD-TRANCAT-CD        PIC 9(04)
--
-- Records MUST be stored / retrieved in primary key order because
-- CBACT04C reads them sequentially and detects account breaks by
-- comparing each row's acct_id to the previous one.
-- The index on acct_id supports the ordered query in TranCatBalJpaRepository.
-- ============================================================

CREATE TABLE carddemo.tran_cat_bal (
    -- FD-TRANCAT-ACCT-ID  PIC 9(11)
    acct_id         CHAR(11)        NOT NULL,

    -- FD-TRANCAT-TYPE-CD  PIC X(02)
    tran_type_cd    CHAR(2)         NOT NULL,

    -- FD-TRANCAT-CD  PIC 9(04)
    tran_cat_cd     SMALLINT        NOT NULL,

    -- Balance for this category/account combination
    -- PIC S9(09)V99  → NUMERIC(11,2)
    tran_cat_bal    NUMERIC(11, 2)  NOT NULL DEFAULT 0,

    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT pk_tran_cat_bal
        PRIMARY KEY (acct_id, tran_type_cd, tran_cat_cd),

    CONSTRAINT fk_tcb_account
        FOREIGN KEY (acct_id)
        REFERENCES carddemo.account (acct_id)
        ON UPDATE CASCADE ON DELETE CASCADE,

    CONSTRAINT chk_tran_cat_cd_range
        CHECK (tran_cat_cd BETWEEN 0 AND 9999)
);

-- Supports the account-break ordered scan (replicates KSDS sequential read order)
CREATE INDEX idx_tran_cat_bal_acct_id
    ON carddemo.tran_cat_bal (acct_id, tran_type_cd, tran_cat_cd);

COMMENT ON TABLE  carddemo.tran_cat_bal           IS 'Per-account, per-category balance used to compute monthly interest. COBOL: CVTRA01Y / TCATBAL-FILE VSAM.';
COMMENT ON COLUMN carddemo.tran_cat_bal.tran_cat_bal IS 'TRAN-CAT-BAL: input to COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200';
