-- ============================================================
-- Flyway migration V5: transaction table
--
-- COBOL source : CVTRA05Y copybook / FD-TRANFILE-REC
-- VSAM file    : TRANSACT-FILE  (sequential, opened OUTPUT by CBACT04C)
--
-- Every row written by CBACT04C will have:
--   tran_type_cd = '01'
--   tran_cat_cd  = '05'
--   tran_source  = 'System'
--   tran_id      = PARM-DATE || LPAD(suffix, 6, '0')   e.g. '2025-04-29000001'
-- ============================================================

CREATE TABLE carddemo.transaction (
    -- TRAN-ID  PIC X(16)  — PARM-DATE(10) + 6-digit suffix
    tran_id             CHAR(16)        NOT NULL,

    -- TRAN-TYPE-CD  PIC X(02)
    tran_type_cd        CHAR(2)         NOT NULL,

    -- TRAN-CAT-CD  PIC X(02)  (stored as text to match COBOL '05')
    tran_cat_cd         CHAR(2)         NOT NULL,

    -- TRAN-SOURCE  PIC X(10)
    tran_source         VARCHAR(10),

    -- TRAN-DESC  PIC X(100)
    tran_desc           VARCHAR(100),

    -- TRAN-AMT  PIC S9(09)V99 COMP-3
    tran_amt            NUMERIC(11, 2)  NOT NULL,

    -- TRAN-MERCHANT-ID  PIC 9(09) — 0 for system-generated transactions
    tran_merchant_id    BIGINT,

    -- TRAN-MERCHANT-NAME  PIC X(50)
    tran_merchant_name  VARCHAR(50),

    -- TRAN-MERCHANT-CITY  PIC X(50)
    tran_merchant_city  VARCHAR(50),

    -- TRAN-MERCHANT-ZIP   PIC X(10)
    tran_merchant_zip   VARCHAR(10),

    -- TRAN-CARD-NUM  PIC X(16)
    tran_card_num       CHAR(16)        NOT NULL,

    -- TRAN-ORIG-TS  (DB2 timestamp format YYYY-MM-DD-HH.MM.SS.HH in COBOL)
    tran_orig_ts        TIMESTAMP       NOT NULL,

    -- TRAN-PROC-TS  (same as orig_ts in CBACT04C)
    tran_proc_ts        TIMESTAMP       NOT NULL,

    created_at          TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT pk_transaction   PRIMARY KEY (tran_id),

    CONSTRAINT fk_tran_card_xref
        FOREIGN KEY (tran_card_num)
        REFERENCES carddemo.card_xref (card_num)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

-- Support lookups by card (common query: "show me all interest charges for card X")
CREATE INDEX idx_tran_card_num  ON carddemo.transaction (tran_card_num);

-- Support range queries by timestamp (batch reconciliation)
CREATE INDEX idx_tran_orig_ts   ON carddemo.transaction (tran_orig_ts);

-- Support filtering by type/category (e.g. find all interest postings)
CREATE INDEX idx_tran_type_cat  ON carddemo.transaction (tran_type_cd, tran_cat_cd);

COMMENT ON TABLE  carddemo.transaction              IS 'Interest and other transactions. COBOL: CVTRA05Y / TRANSACT-FILE VSAM. Written sequentially by CBACT04C.';
COMMENT ON COLUMN carddemo.transaction.tran_id      IS 'TRAN-ID PIC X(16): PARM-DATE(10) || LPAD(suffix,6,"0")';
COMMENT ON COLUMN carddemo.transaction.tran_amt     IS 'Monthly interest: (tran_cat_bal * dis_int_rate) / 1200';
COMMENT ON COLUMN carddemo.transaction.tran_orig_ts IS 'Maps to DB2-FORMAT-TS built by Z-GET-DB2-FORMAT-TIMESTAMP in COBOL';
