-- =============================================================================
-- V5__seed_audit_log.sql
-- Populates audit_log table with historical entries (H2 compatible)
--
-- COBOL equivalent:
--   WRITE AUDITLOG FROM WS-AUDIT-RECORD
--   (after each successful REWRITE ACCTDAT)
--
-- H2 notes:
--   - No json_build_object() - use string concatenation
--   - CLOB used instead of JSONB
--   - No DO $$ blocks - use plain INSERT statements
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Clean existing seed audit entries (idempotent)
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
-- ACCOUNT 00001001001 - ALICE JOHNSON audit trail
-- COBOL: initial WRITE + multiple REWRITE operations
-- =============================================================================

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00001001001', 'CREATE',
    'SYSTEM', '2022-01-15 09:00:00',
    NULL,
    '{"accountId":"00001001001","accountName":"ALICE JOHNSON","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":5000.00,"cashCreditLimit":2000.00,"openDate":"2022-01-15","overLimitInd":"N"}',
    '10.0.0.1', 'SYS-INIT-001'
);

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00001001001', 'UPDATE',
    'ops', '2022-06-10 11:00:00',
    '{"accountId":"00001001001","addrLine1":"100 Test St","addrZip":"10000","version":0}',
    '{"accountId":"00001001001","addrLine1":"123 Madison Avenue","addrLine2":"Apt 4B","addrZip":"10001","version":1}',
    '10.0.0.2', 'OPS-SES-0022'
);

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00001001001', 'UPDATE',
    'admin', '2023-01-20 14:30:00',
    '{"accountId":"00001001001","creditLimit":3000.00,"cashCreditLimit":1500.00,"version":1}',
    '{"accountId":"00001001001","creditLimit":5000.00,"cashCreditLimit":2000.00,"version":2}',
    '10.0.0.5', 'ADM-SES-0101'
);

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00001001001', 'UPDATE',
    'admin', '2024-03-10 11:00:00',
    '{"accountId":"00001001001","currBal":950.50,"currCycleCredit":400.00,"currCycleDebit":200.00,"version":2}',
    '{"accountId":"00001001001","currBal":1250.75,"currCycleCredit":500.00,"currCycleDebit":250.25,"version":3}',
    '10.0.0.5', 'ADM-SES-0210'
);

-- =============================================================================
-- ACCOUNT 00001001004 - DAVID BROWN (over-limit triggered)
-- COBOL: COMPUTE OVER-LIMIT-IND = 'Y' after REWRITE
-- =============================================================================

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00001001004', 'CREATE',
    'SYSTEM', '2019-11-05 07:30:00',
    NULL,
    '{"accountId":"00001001004","accountName":"DAVID BROWN","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":5000.00,"overLimitInd":"N"}',
    '10.0.0.1', 'SYS-INIT-004'
);

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00001001004', 'UPDATE',
    'ops', '2024-06-01 16:00:00',
    '{"accountId":"00001001004","currBal":4900.00,"creditLimit":5000.00,"overLimitInd":"N","version":11}',
    '{"accountId":"00001001004","currBal":5750.00,"creditLimit":5000.00,"overLimitInd":"Y","version":12}',
    '10.0.0.3', 'OPS-SES-0441'
);

-- =============================================================================
-- ACCOUNT 00001002003 - HENRY WILSON (deactivation history)
-- COBOL: MOVE 'N' TO ACCT-ACTIVE-STATUS + REWRITE ACCTDAT
-- =============================================================================

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00001002003', 'CREATE',
    'SYSTEM', '2015-03-20 08:00:00',
    NULL,
    '{"accountId":"00001002003","accountName":"HENRY WILSON","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":20000.00,"overLimitInd":"N"}',
    '10.0.0.1', 'SYS-INIT-010'
);

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00001002003', 'UPDATE',
    'admin', '2020-05-15 10:00:00',
    '{"accountId":"00001002003","creditLimit":15000.00,"version":4}',
    '{"accountId":"00001002003","creditLimit":20000.00,"version":5}',
    '10.0.0.5', 'ADM-SES-0055'
);

-- Logical DELETE - COBOL: MOVE 'N' TO ACCT-ACTIVE-STATUS
INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00001002003', 'DELETE',
    'admin', '2023-01-10 09:00:00',
    '{"accountId":"00001002003","accountName":"HENRY WILSON","activeStatus":"Y","currBal":0.00,"creditLimit":20000.00,"version":9}',
    NULL,
    '10.0.0.5', 'ADM-SES-0312'
);

-- =============================================================================
-- ACCOUNT 00002001003 - MIA DAVIS (deactivation)
-- =============================================================================

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00002001003', 'CREATE',
    'SYSTEM', '2019-04-10 07:00:00',
    NULL,
    '{"accountId":"00002001003","accountName":"MIA DAVIS","accountType":"2","activeStatus":"Y","currBal":0.00,"creditLimit":0.00,"overLimitInd":"N"}',
    '10.0.0.1', 'SYS-INIT-015'
);

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00002001003', 'DELETE',
    'admin', '2022-01-15 09:00:00',
    '{"accountId":"00002001003","accountName":"MIA DAVIS","activeStatus":"Y","currBal":0.00,"version":5}',
    NULL,
    '10.0.0.5', 'ADM-SES-0198'
);

-- =============================================================================
-- ACCOUNT 00002002001 - PEAK SOLUTIONS (business audit trail)
-- =============================================================================

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00002002001', 'CREATE',
    'SYSTEM', '2018-01-10 08:00:00',
    NULL,
    '{"accountId":"00002002001","accountName":"PEAK SOLUTIONS","accountType":"2","activeStatus":"Y","currBal":0.00,"creditLimit":0.00}',
    '10.0.0.1', 'SYS-INIT-020'
);

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00002002001', 'UPDATE',
    'admin', '2020-06-15 14:00:00',
    '{"accountId":"00002002001","addrLine1":"Old Address","version":10}',
    '{"accountId":"00002002001","addrLine1":"999 Corporate Plaza","addrLine2":"Suite 500","version":11}',
    '10.0.0.5', 'ADM-SES-0150'
);

INSERT INTO audit_log (
    entity_type, entity_id, action,
    changed_by, changed_at,
    old_value, new_value,
    ip_address, session_id
) VALUES (
    'ACCOUNT', '00002002001', 'UPDATE',
    'admin', '2024-10-01 09:00:00',
    '{"accountId":"00002002001","currBal":38000.00,"currCycleCredit":20000.00,"version":21}',
    '{"accountId":"00002002001","currBal":45000.00,"currCycleCredit":25000.00,"version":22}',
    '10.0.0.5', 'ADM-SES-0890'
);

-- =============================================================================
-- Bulk CREATE audit entries for remaining seeded accounts
-- H2 compatible - individual INSERT statements (no json_build_object)
-- COBOL: initial WRITE ACCTDAT entries for each account
-- =============================================================================

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00001001002','CREATE','SYSTEM','2021-06-20 10:00:00',NULL,'{"accountId":"00001001002","accountName":"BOB MARTINEZ","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":6000.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00001001003','CREATE','SYSTEM','2020-03-10 08:00:00',NULL,'{"accountId":"00001001003","accountName":"CAROL WILLIAMS","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":5000.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00001001005','CREATE','admin','2024-01-01 12:00:00',NULL,'{"accountId":"00001001005","accountName":"EVE TAYLOR","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":3000.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00001002001','CREATE','SYSTEM','2018-07-01 09:00:00',NULL,'{"accountId":"00001002001","accountName":"FRANK MILLER","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":25000.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00001002002','CREATE','SYSTEM','2017-09-15 10:30:00',NULL,'{"accountId":"00001002002","accountName":"GRACE CHEN","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":25000.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00001002004','CREATE','SYSTEM','2016-05-12 11:00:00',NULL,'{"accountId":"00001002004","accountName":"IRIS ANDERSON","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":20000.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00001002005','CREATE','admin','2023-08-01 14:00:00',NULL,'{"accountId":"00001002005","accountName":"JACK THOMAS","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":15000.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00002001001','CREATE','SYSTEM','2021-02-14 09:00:00',NULL,'{"accountId":"00002001001","accountName":"KATE ROBINSON","accountType":"2","activeStatus":"Y","currBal":0.00,"creditLimit":0.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00002001002','CREATE','admin','2023-09-01 08:00:00',NULL,'{"accountId":"00002001002","accountName":"LEO JACKSON","accountType":"2","activeStatus":"Y","currBal":0.00,"creditLimit":0.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00002001004','CREATE','SYSTEM','2020-11-20 10:30:00',NULL,'{"accountId":"00002001004","accountName":"NOAH GARCIA","accountType":"2","activeStatus":"Y","currBal":0.00,"creditLimit":0.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00002001005','CREATE','admin','2024-06-01 13:00:00',NULL,'{"accountId":"00002001005","accountName":"OLIVIA LEE","accountType":"2","activeStatus":"Y","currBal":0.00,"creditLimit":0.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00002002002','CREATE','SYSTEM','2015-06-15 09:00:00',NULL,'{"accountId":"00002002002","accountName":"SUNSET TRADING","accountType":"2","activeStatus":"Y","currBal":0.00,"creditLimit":0.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00002002003','CREATE','SYSTEM','2016-03-22 07:30:00',NULL,'{"accountId":"00002002003","accountName":"GLOBAL VENTURES","accountType":"2","activeStatus":"Y","currBal":0.00,"creditLimit":0.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00003001001','CREATE','SYSTEM','2022-05-10 10:00:00',NULL,'{"accountId":"00003001001","accountName":"PETER NGUYEN","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":7500.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00003001002','CREATE','SYSTEM','2021-08-25 09:00:00',NULL,'{"accountId":"00003001002","accountName":"QUINN HARRIS","accountType":"2","activeStatus":"Y","currBal":0.00,"creditLimit":0.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00003001003','CREATE','SYSTEM','2020-12-01 08:00:00',NULL,'{"accountId":"00003001003","accountName":"ROSE CLARK","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":8000.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00004001001','CREATE','SYSTEM','2019-06-01 09:00:00',NULL,'{"accountId":"00004001001","accountName":"SAM LEWIS","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":4000.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00004001002','CREATE','admin','2024-01-15 11:00:00',NULL,'{"accountId":"00004001002","accountName":"TINA WALKER","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":5000.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00005001001','CREATE','SYSTEM','2022-03-01 09:00:00',NULL,'{"accountId":"00005001001","accountName":"URSULA KING","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":6000.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00005001002','CREATE','SYSTEM','2021-07-04 08:00:00',NULL,'{"accountId":"00005001002","accountName":"VICTOR SCOTT","accountType":"1","activeStatus":"Y","currBal":0.00,"creditLimit":9000.00}','10.0.0.1','SYS-BULK-SEED');

INSERT INTO audit_log (entity_type, entity_id, action, changed_by, changed_at, old_value, new_value, ip_address, session_id)
VALUES ('ACCOUNT','00005001003','CREATE','SYSTEM','2018-11-11 07:00:00',NULL,'{"accountId":"00005001003","accountName":"WENDY HILL","accountType":"2","activeStatus":"Y","currBal":0.00,"creditLimit":0.00}','10.0.0.1','SYS-BULK-SEED');