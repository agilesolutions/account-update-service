-- ============================================================
-- Flyway migration V3: disclosure_group table
--
-- COBOL source : CVTRA02Y copybook / FD-DISCGRP-REC
-- VSAM file    : DISCGRP-FILE  (KSDS, random reads)
-- Composite PK : FD-DIS-ACCT-GROUP-ID  PIC X(10)
--              + FD-DIS-TRAN-TYPE-CD   PIC X(02)
--              + FD-DIS-TRAN-CAT-CD    PIC 9(04)
--
-- CBACT04C logic:
--   1. Look up specific group → if not found (status '23')
--   2. Re-read with acct_group_id = 'DEFAULT'
-- The DEFAULT row must always exist for each (tran_type_cd, tran_cat_cd) pair.
-- ============================================================

CREATE TABLE carddemo.disclosure_group (
    -- FD-DIS-ACCT-GROUP-ID  PIC X(10) — 'DEFAULT' is a reserved sentinel value
    acct_group_id   CHAR(10)        NOT NULL,

    -- FD-DIS-TRAN-TYPE-CD   PIC X(02)
    tran_type_cd    CHAR(2)         NOT NULL,

    -- FD-DIS-TRAN-CAT-CD    PIC 9(04)
    tran_cat_cd     SMALLINT        NOT NULL,

    -- DIS-INT-RATE — annual percentage rate, e.g. 18.0000 = 18% APR
    -- CBACT04C: COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
    dis_int_rate    NUMERIC(8, 4)   NOT NULL DEFAULT 0,

    description     VARCHAR(100),

    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT pk_disclosure_group
        PRIMARY KEY (acct_group_id, tran_type_cd, tran_cat_cd),

    CONSTRAINT chk_dis_int_rate_positive
        CHECK (dis_int_rate >= 0),

    CONSTRAINT chk_tran_cat_cd_range
        CHECK (tran_cat_cd BETWEEN 0 AND 9999)
);

COMMENT ON TABLE  carddemo.disclosure_group              IS 'Interest rate table by account group and transaction category. COBOL: CVTRA02Y / DISCGRP-FILE VSAM.';
COMMENT ON COLUMN carddemo.disclosure_group.acct_group_id IS 'FD-DIS-ACCT-GROUP-ID PIC X(10). "DEFAULT" is fallback when specific group missing.';
COMMENT ON COLUMN carddemo.disclosure_group.dis_int_rate  IS 'Annual interest rate (APR). CBACT04C divides by 1200 for monthly rate.';

-- Seed the mandatory DEFAULT rows — one per transaction category used by CBACT04C.
-- Override these per-group in V6 seed migration.
INSERT INTO carddemo.disclosure_group (acct_group_id, tran_type_cd, tran_cat_cd, dis_int_rate, description)
VALUES
    ('DEFAULT   ', 'PR', 1, 19.9900, 'Default rate — purchases'),
    ('DEFAULT   ', 'PR', 2, 19.9900, 'Default rate — purchases cat 2'),
    ('DEFAULT   ', 'CR', 1,  0.0000, 'Default rate — credits (no interest)'),
    ('DEFAULT   ', 'CR', 2,  0.0000, 'Default rate — credits cat 2'),
    ('DEFAULT   ', '01', 5,  0.0000, 'Default rate — system interest postings');
