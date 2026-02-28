-- =============================================================================
-- V6__seed_audit_log.sql
-- Populates audit_log table with historical audit trail entries
--
-- COBOL equivalent:
--   WRITE AUDITLOG FROM WS-AUDIT-RECORD
--   (written after each successful REWRITE ACCTDAT)
--
-- Actions mirror COBOL operations:
--   CREATE -> COBOL WRITE ACCTDAT
--   UPDATE -> COBOL REWRITE ACCTDAT
--   DELETE -> COBOL MOVE 'N' TO ACCT-ACTIVE-STATUS + REWRITE ACCTDAT
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Clean existing seed audit entries (idempotent re-run safety)
-- -----------------------------------------------------------------------------
DELETE FROM audit_log
WHERE entity_type = 'ACCOUNT'
  AND entity_id IN (
    '00001001001','00001001002','00001001003','00001001004','00001001005',
    '00001002001','00001002002','00001002003','00001002004','00001002005',
    '00002001001','00002001002','00002001003','00002001004','00002001005',
    '00002002001','00002002002','00002002003',
    '00003001001','00003001002','00003001003',
    '00004001001','00004001002',
    '00005001001','00005001002','00005001003'
);

-- =============================================================================
-- ACCOUNT: 00001001001 - ALICE JOHNSON
-- COBOL: audit trail from initial WRITE through multiple REWRITE operations
-- =============================================================================

INSERT INTO audit_log (
    entity_type, entity_id, action, changed_by, changed_at,
    old_value, new_value, ip_address, session_id
) VALUES

-- Original CREATE (COBOL: WRITE ACCTDAT)
(
    'ACCOUNT', '00001001001', 'CREATE',
    'SYSTEM',
    '2022-01-15 09:00:00',
    NULL,
    '{"accountId":"00001001001","accountName":"ALICE JOHNSON","accountType":"1",'
    '"activeStatus":"ACTIVE","currBal":0.00,"creditLimit":5000.00,'
    '"cashCreditLimit":2000.00,"openDate":"2022-01-15","overLimitInd":"N"}',
    '10.0.0.1',
    'SYS-INIT-001'
),

-- First UPDATE - address change (COBOL: REWRITE ACCTDAT)
(
    'ACCOUNT', '00001001001', 'UPDATE',
    'ops',
    '2022-06-10 11:00:00',
    '{"accountId":"00001001001","accountName":"ALICE JOHNSON","addrLine1":"100 Test St",'
    '"addrZip":"10000","version":0}',
    '{"accountId":"00001001001","accountName":"ALICE JOHNSON","addrLine1":"123 Madison Avenue",'
    '"addrLine2":"Apt 4B","addrZip":"10001","version":1}',
    '10.0.0.2',
    'OPS-SES-0022'
),

-- Second UPDATE - credit limit increase (COBOL: REWRITE ACCTDAT)
(
    'ACCOUNT', '00001001001', 'UPDATE',
    'admin',
    '2023-01-20 14:30:00',
    '{"accountId":"00001001001","creditLimit":3000.00,"cashCreditLimit":1500.00,"version":1}',
    '{"accountId":"00001001001","creditLimit":5000.00,"cashCreditLimit":2000.00,"version":2}',
    '10.0.0.5',
    'ADM-SES-0101'
),

-- Third UPDATE - balance update after cycle close (COBOL: REWRITE ACCTDAT)
(
    'ACCOUNT', '00001001001', 'UPDATE',
    'admin',
    '2024-03-10 11:00:00',
    '{"accountId":"00001001001","currBal":950.50,"currCycleCredit":400.00,"currCycleDebit":200.00,"version":2}',
    '{"accountId":"00001001001","currBal":1250.75,"currCycleCredit":500.00,"currCycleDebit":250.25,"version":3}',
    '10.0.0.5',
    'ADM-SES-0210'
);

-- =============================================================================
-- ACCOUNT: 00001001004 - DAVID BROWN (over-limit history)
-- =============================================================================

INSERT INTO audit_log (
    entity_type, entity_id, action, changed_by, changed_at,
    old_value, new_value, ip_address, session_id
) VALUES

(
    'ACCOUNT', '00001001004', 'CREATE',
    'SYSTEM',
    '2019-11-05 07:30:00',
    NULL,
    '{"accountId":"00001001004","accountName":"DAVID BROWN","accountType":"1",'
    '"activeStatus":"ACTIVE","currBal":0.00,"creditLimit":5000.00,"overLimitInd":"N"}',
    '10.0.0.1',
    'SYS-INIT-004'
),

-- Over-limit flag triggered (COBOL: COMPUTE OVER-LIMIT-IND = 'Y')
(
    'ACCOUNT', '00001001004', 'UPDATE',
    'ops',
    '2024-06-01 16:00:00',
    '{"accountId":"00001001004","currBal":4900.00,"creditLimit":5000.00,"overLimitInd":"N","version":11}',
    '{"accountId":"00001001004","currBal":5750.00,"creditLimit":5000.00,"overLimitInd":"Y","version":12}',
    '10.0.0.3',
    'OPS-SES-0441'
);

-- =============================================================================
-- ACCOUNT: 00001002003 - HENRY WILSON (deactivation history)
-- COBOL: MOVE 'N' TO ACCT-ACTIVE-STATUS + REWRITE ACCTDAT
-- =============================================================================

INSERT INTO audit_log (
    entity_type, entity_id, action, changed_by, changed_at,
    old_value, new_value, ip_address, session_id
) VALUES

(
    'ACCOUNT', '00001002003', 'CREATE',
    'SYSTEM',
    '2015-03-20 08:00:00',
    NULL,
    '{"accountId":"00001002003","accountName":"HENRY WILSON","accountType":"1",'
    '"activeStatus":"ACTIVE","currBal":0.00,"creditLimit":20000.00,"overLimitInd":"N"}',
    '10.0.0.1',
    'SYS-INIT-010'
),

(
    'ACCOUNT', '00001002003', 'UPDATE',
    'admin',
    '2020-05-15 10:00:00',
    '{"accountId":"00001002003","creditLimit":15000.00,"version":4}',
    '{"accountId":"00001002003","creditLimit":20000.00,"version":5}',
    '10.0.0.5',
    'ADM-SES-0055'
),

-- Logical DELETE - account deactivated
-- COBOL: MOVE 'N' TO ACCT-ACTIVE-STATUS, REWRITE ACCTDAT
(
    'ACCOUNT', '00001002003', 'DELETE',
    'admin',
    '2023-01-10 09:00:00',
    '{"accountId":"00001002003","accountName":"HENRY WILSON","activeStatus":"ACTIVE",'
    '"currBal":0.00,"creditLimit":20000.00,"version":9}',
    NULL,
    '10.0.0.5',
    'ADM-SES-0312'
);

-- =============================================================================
-- ACCOUNT: 00002001003 - MIA DAVIS (deactivation)
-- =============================================================================

INSERT INTO audit_log (
    entity_type, entity_id, action, changed_by, changed_at,
    old_value, new_value, ip_address, session_id
) VALUES

(
    'ACCOUNT', '00002001003', 'CREATE',
    'SYSTEM',
    '2019-04-10 07:00:00',
    NULL,
    '{"accountId":"00002001003","accountName":"MIA DAVIS","accountType":"2",'
    '"activeStatus":"ACTIVE","currBal":0.00,"creditLimit":0.00,"overLimitInd":"N"}',
    '10.0.0.1',
    'SYS-INIT-015'
),

(
    'ACCOUNT', '00002001003', 'DELETE',
    'admin',
    '2022-01-15 09:00:00',
    '{"accountId":"00002001003","accountName":"MIA DAVIS","activeStatus":"ACTIVE",'
    '"currBal":0.00,"version":5}',
    NULL,
    '10.0.0.5',
    'ADM-SES-0198'
);

-- =============================================================================
-- ACCOUNT: 00002002001 - PEAK SOLUTIONS (business account audit trail)
-- =============================================================================

INSERT INTO audit_log (
    entity_type, entity_id, action, changed_by, changed_at,
    old_value, new_value, ip_address, session_id
) VALUES

(
    'ACCOUNT', '00002002001', 'CREATE',
    'SYSTEM',
    '2018-01-10 08:00:00',
    NULL,
    '{"accountId":"00002002001","accountName":"PEAK SOLUTIONS","accountType":"2",'
    '"activeStatus":"ACTIVE","currBal":0.00,"creditLimit":0.00}',
    '10.0.0.1',
    'SYS-INIT-020'
),

(
    'ACCOUNT', '00002002001', 'UPDATE',
    'admin',
    '2020-06-15 14:00:00',
    '{"accountId":"00002002001","accountName":"PEAK SOLUTIONS","addrLine1":"Old Address","version":10}',
    '{"accountId":"00002002001","accountName":"PEAK SOLUTIONS","addrLine1":"999 Corporate Plaza","addrLine2":"Suite 500","version":11}',
    '10.0.0.5',
    'ADM-SES-0150'
),

(
    'ACCOUNT', '00002002001', 'UPDATE',
    'admin',
    '2024-10-01 09:00:00',
    '{"accountId":"00002002001","currBal":38000.00,"currCycleCredit":20000.00,"version":21}',
    '{"accountId":"00002002001","currBal":45000.00,"currCycleCredit":25000.00,"version":22}',
    '10.0.0.5',
    'ADM-SES-0890'
);

-- =============================================================================
-- Bulk CREATE audit entries for remaining seeded accounts
-- COBOL: initial WRITE ACCTDAT for each new account record
-- =============================================================================

INSERT INTO audit_log (
    entity_type, entity_id, action, changed_by, changed_at,
    old_value, new_value, ip_address, session_id
)
SELECT
    'ACCOUNT',
    a.account_id,
    'CREATE',
    a.created_by,
    a.created_at,
    NULL,
    json_build_object(
        'accountId',         a.account_id,
        'accountName',       a.account_name,
        'accountType',       a.account_type,
        'activeStatus',      a.active_status,
        'currBal',           a.curr_bal,
        'creditLimit',       a.credit_limit,
        'cashCreditLimit',   a.cash_credit_limit,
        'openDate',          a.open_date,
        'expiryDate',        a.expiry_date,
        'addrLine1',         a.addr_line1,
        'addrState',         a.addr_state,
        'addrCountry',       a.addr_country,
        'overLimitInd',      a.over_limit_ind
    )::text,
    '10.0.0.1',
    'SYS-BULK-SEED'
FROM accounts a
WHERE a.account_id IN (
    '00001001002','00001001003','00001001005',
    '00001002001','00001002002','00001002004','00001002005',
    '00002001001','00002001002','00002001004','00002001005',
    '00002002002','00002002003',
    '00003001001','00003001002','00003001003',
    '00004001001','00004001002',
    '00005001001','00005001002','00005001003'
)
-- Only insert if no CREATE entry already exists for that account
AND NOT EXISTS (
    SELECT 1 FROM audit_log al
    WHERE al.entity_type = 'ACCOUNT'
      AND al.entity_id   = a.account_id
      AND al.action      = 'CREATE'
);