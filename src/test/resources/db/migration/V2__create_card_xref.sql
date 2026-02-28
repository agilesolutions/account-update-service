-- ============================================================
-- Flyway migration V2: card_xref table
--
-- COBOL source : CVACT03Y copybook / FD-XREFFILE-REC
-- VSAM file    : XREFFILE  (KSDS)
--                PRIMARY KEY   : FD-XREF-CARD-NUM  PIC X(16)
--                ALTERNATE KEY : FD-XREF-ACCT-ID   PIC 9(11)
--
-- The ALTERNATE RECORD KEY becomes a UNIQUE INDEX in PostgreSQL.
-- CBACT04C accesses this file by the alternate key only
-- (READ XREF-FILE KEY IS FD-XREF-ACCT-ID).
-- ============================================================

CREATE TABLE carddemo.card_xref (
    -- FD-XREF-CARD-NUM  PIC X(16)  — VSAM primary key
    card_num    CHAR(16)    NOT NULL,

    -- FD-XREF-CUST-NUM  PIC 9(09)
    cust_num    BIGINT      NOT NULL,

    -- FD-XREF-ACCT-ID   PIC 9(11)  — VSAM alternate key; unique in carddemo
    acct_id     CHAR(11)    NOT NULL,

    created_at  TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT pk_card_xref  PRIMARY KEY (card_num),
    CONSTRAINT fk_xref_acct  FOREIGN KEY (acct_id)
                             REFERENCES carddemo.account (acct_id)
                             ON UPDATE CASCADE ON DELETE RESTRICT
);

-- Replicates the VSAM ALTERNATE RECORD KEY behaviour
CREATE UNIQUE INDEX uq_card_xref_acct_id ON carddemo.card_xref (acct_id);

COMMENT ON TABLE  carddemo.card_xref         IS 'Card-to-account cross-reference. COBOL: CVACT03Y / XREFFILE VSAM.';
COMMENT ON COLUMN carddemo.card_xref.card_num IS 'FD-XREF-CARD-NUM PIC X(16) — VSAM primary key';
COMMENT ON COLUMN carddemo.card_xref.acct_id  IS 'FD-XREF-ACCT-ID PIC 9(11)  — VSAM alternate key (unique index)';
