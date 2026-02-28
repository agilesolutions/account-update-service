-- =============================================================================
-- V4__seed_accounts.sql
-- Populates accounts table with representative test data (H2 compatible)
--
-- COBOL equivalent:
--   WRITE ACCTDAT FROM WS-ACCOUNT-MASTER-RECORD
--
-- Account ID format: PIC X(11) - 11 numeric digits
-- Account Types: '1' = Credit, '2' = Debit
-- Active Status: 'Y' = ACTIVE, 'N' = INACTIVE
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Clean existing seed data (idempotent)
-- -----------------------------------------------------------------------------
DELETE FROM accounts
WHERE account_id IN (
    '00001001001','00001001002','00001001003','00001001004','00001001005',
    '00001002001','00001002002','00001002003','00001002004','00001002005',
    '00002001001','00002001002','00002001003','00002001004','00002001005',
    '00002002001','00002002002','00002002003',
    '00003001001','00003001002','00003001003',
    '00004001001','00004001002',
    '00005001001','00005001002','00005001003'
);

-- =============================================================================
-- BRANCH 00001 - CREDIT ACCOUNTS - GROUP 001 (Standard Credit)
-- COBOL: ACCT-TYPE-CD = '1', ACCT-ACTIVE-STATUS = 'Y'
-- =============================================================================

-- Account 1: Standard credit, within limit
-- COBOL: ACCT-CURR-BAL < ACCT-CREDIT-LIMIT -> OVER-LIMIT-IND = 'N'
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00001001001', 'ALICE JOHNSON', '1', 'Y',
    1250.75, 5000.00, 2000.00,
    '2022-01-15', '2027-01-15', '2025-01-15',
    500.00, 250.25,
    '10001', 'NY', 'USA',
    '123 Madison Avenue', 'Apt 4B',
    '+12125551001', '+12125551002',
    'GRP001', 'N', 'N',
    'SYSTEM', '2022-01-15 09:00:00', 'admin', '2024-03-10 11:00:00', 3
);

-- Account 2: Standard credit, moderate balance
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00001001002', 'BOB MARTINEZ', '1', 'Y',
    3100.00, 6000.00, 2500.00,
    '2021-06-20', '2026-06-20', '2024-06-20',
    1200.00, 900.00,
    '10002', 'NY', 'USA',
    '456 Park Avenue', 'Floor 12',
    '+12125552001', '+12125552002',
    'GRP001', 'N', 'N',
    'SYSTEM', '2021-06-20 10:00:00', 'admin', '2024-04-15 14:30:00', 5
);

-- Account 3: High balance, approaching limit
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00001001003', 'CAROL WILLIAMS', '1', 'Y',
    4750.00, 5000.00, 2000.00,
    '2020-03-10', '2025-03-10', '2023-03-10',
    2000.00, 1750.00,
    '10003', 'NY', 'USA',
    '789 Lexington Ave', NULL,
    '+12125553001', NULL,
    'GRP001', 'N', 'N',
    'SYSTEM', '2020-03-10 08:00:00', 'ops', '2024-05-20 09:15:00', 8
);

-- Account 4: OVER LIMIT
-- COBOL: ACCT-CURR-BAL > ACCT-CREDIT-LIMIT -> OVER-LIMIT-IND = 'Y'
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00001001004', 'DAVID BROWN', '1', 'Y',
    5750.00, 5000.00, 2000.00,
    '2019-11-05', '2024-11-05', '2022-11-05',
    2500.00, 3250.00,
    '10004', 'NY', 'USA',
    '321 Fifth Avenue', 'Suite 100',
    '+12125554001', '+12125554002',
    'GRP001', 'N', 'Y',
    'SYSTEM', '2019-11-05 07:30:00', 'ops', '2024-06-01 16:00:00', 12
);

-- Account 5: Zero balance, new account, student
-- COBOL: ACCT-CURR-BAL = ZERO, ACCT-STUDENT-IND = 'Y'
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00001001005', 'EVE TAYLOR', '1', 'Y',
    0.00, 3000.00, 1500.00,
    '2024-01-01', '2029-01-01', '2027-01-01',
    0.00, 0.00,
    '10005', 'NY', 'USA',
    '654 Broadway', 'Apt 2A',
    '+12125555001', NULL,
    'GRP001', 'Y', 'N',
    'admin', '2024-01-01 12:00:00', NULL, NULL, 0
);

-- =============================================================================
-- BRANCH 00001 - CREDIT ACCOUNTS - GROUP 002 (Premium Credit)
-- =============================================================================

-- Account 6: Premium credit, high limit low utilisation
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00001002001', 'FRANK MILLER', '1', 'Y',
    5000.00, 25000.00, 10000.00,
    '2018-07-01', '2028-07-01', '2026-07-01',
    3000.00, 2000.00,
    '10006', 'NY', 'USA',
    '111 Wall Street', 'Floor 30',
    '+12125556001', '+12125556002',
    'GRP002', 'N', 'N',
    'SYSTEM', '2018-07-01 09:00:00', 'admin', '2024-07-01 10:00:00', 15
);

-- Account 7: Premium credit, over limit
-- COBOL: OVER-LIMIT-IND = 'Y'
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00001002002', 'GRACE CHEN', '1', 'Y',
    26500.00, 25000.00, 10000.00,
    '2017-09-15', '2027-09-15', '2025-09-15',
    15000.00, 16500.00,
    '10010', 'NY', 'USA',
    '222 Park Place', 'Penthouse',
    '+12125556101', '+12125556102',
    'GRP002', 'N', 'Y',
    'SYSTEM', '2017-09-15 10:30:00', 'admin', '2024-08-10 14:00:00', 20
);

-- Account 8: Premium credit, INACTIVE (closed)
-- COBOL: ACCT-ACTIVE-STATUS = 'N'
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00001002003', 'HENRY WILSON', '1', 'N',
    0.00, 20000.00, 8000.00,
    '2015-03-20', '2025-03-20', '2023-03-20',
    0.00, 0.00,
    '10011', 'NY', 'USA',
    '333 Riverside Drive', NULL,
    '+12125556201', NULL,
    'GRP002', 'N', 'N',
    'SYSTEM', '2015-03-20 08:00:00', 'admin', '2023-01-10 09:00:00', 10
);

-- Account 9: Premium credit, recently reissued
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00001002004', 'IRIS ANDERSON', '1', 'Y',
    8750.50, 20000.00, 8000.00,
    '2016-05-12', '2026-05-12', '2024-05-12',
    5000.00, 4250.50,
    '10012', 'NY', 'USA',
    '444 Central Park West', 'Apt 15C',
    '+12125556301', '+12125556302',
    'GRP002', 'N', 'N',
    'SYSTEM', '2016-05-12 11:00:00', 'ops', '2024-05-12 08:30:00', 18
);

-- Account 10: Premium credit, student upgrade
-- COBOL: ACCT-STUDENT-IND = 'Y'
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00001002005', 'JACK THOMAS', '1', 'Y',
    2100.00, 15000.00, 6000.00,
    '2023-08-01', '2028-08-01', '2026-08-01',
    1000.00, 1100.00,
    '10013', 'NY', 'USA',
    '555 Amsterdam Avenue', 'Room 210',
    '+12125556401', NULL,
    'GRP002', 'Y', 'N',
    'admin', '2023-08-01 14:00:00', NULL, NULL, 1
);

-- =============================================================================
-- BRANCH 00002 - DEBIT ACCOUNTS - GROUP 001
-- COBOL: ACCT-TYPE-CD = '2'
-- =============================================================================

-- Account 11: Debit, healthy balance
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00002001001', 'KATE ROBINSON', '2', 'Y',
    3500.00, 0.00, 0.00,
    '2021-02-14', '2026-02-14', '2024-02-14',
    1500.00, 500.00,
    '60601', 'IL', 'USA',
    '100 Michigan Avenue', 'Apt 8A',
    '+13125557001', '+13125557002',
    'GRP003', 'N', 'N',
    'SYSTEM', '2021-02-14 09:00:00', 'admin', '2024-02-14 10:00:00', 4
);

-- Account 12: Debit, student
-- COBOL: ACCT-STUDENT-IND = 'Y'
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00002001002', 'LEO JACKSON', '2', 'Y',
    750.25, 0.00, 0.00,
    '2023-09-01', '2028-09-01', '2026-09-01',
    500.00, 249.75,
    '60602', 'IL', 'USA',
    '200 State Street', 'Dorm 302',
    '+13125558001', NULL,
    'GRP003', 'Y', 'N',
    'admin', '2023-09-01 08:00:00', NULL, NULL, 0
);

-- Account 13: Debit, INACTIVE
-- COBOL: ACCT-ACTIVE-STATUS = 'N'
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00002001003', 'MIA DAVIS', '2', 'N',
    0.00, 0.00, 0.00,
    '2019-04-10', '2024-04-10', '2022-04-10',
    0.00, 0.00,
    '60603', 'IL', 'USA',
    '300 Wacker Drive', NULL,
    '+13125559001', NULL,
    'GRP003', 'N', 'N',
    'SYSTEM', '2019-04-10 07:00:00', 'admin', '2022-01-15 09:00:00', 6
);

-- Account 14: Debit, large balance
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00002001004', 'NOAH GARCIA', '2', 'Y',
    15200.00, 0.00, 0.00,
    '2020-11-20', '2025-11-20', '2023-11-20',
    8000.00, 3000.00,
    '60604', 'IL', 'USA',
    '400 Lake Shore Drive', 'Apt 22B',
    '+13125550001', '+13125550002',
    'GRP003', 'N', 'N',
    'SYSTEM', '2020-11-20 10:30:00', 'ops', '2024-09-01 11:00:00', 9
);

-- Account 15: Debit, minimal balance
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00002001005', 'OLIVIA LEE', '2', 'Y',
    50.00, 0.00, 0.00,
    '2024-06-01', '2029-06-01', '2027-06-01',
    50.00, 0.00,
    '60605', 'IL', 'USA',
    '500 Navy Pier Way', 'Unit 1',
    '+13125551111', NULL,
    'GRP003', 'Y', 'N',
    'admin', '2024-06-01 13:00:00', NULL, NULL, 0
);

-- =============================================================================
-- BRANCH 00002 - DEBIT ACCOUNTS - GROUP 002 (Business Debit)
-- =============================================================================

-- Account 16: Business debit, active high volume
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00002002001', 'PEAK SOLUTIONS', '2', 'Y',
    45000.00, 0.00, 0.00,
    '2018-01-10', '2028-01-10', '2026-01-10',
    25000.00, 15000.00,
    '10020', 'NY', 'USA',
    '999 Corporate Plaza', 'Suite 500',
    '+12125560001', '+12125560002',
    'GRP004', 'N', 'N',
    'SYSTEM', '2018-01-10 08:00:00', 'admin', '2024-10-01 09:00:00', 22
);

-- Account 17: Business debit, INACTIVE (dissolved)
-- COBOL: ACCT-ACTIVE-STATUS = 'N'
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00002002002', 'SUNSET TRADING', '2', 'N',
    0.00, 0.00, 0.00,
    '2015-06-15', '2025-06-15', '2023-06-15',
    0.00, 0.00,
    '10021', 'NY', 'USA',
    '888 Commerce Street', 'Floor 2',
    '+12125561001', NULL,
    'GRP004', 'N', 'N',
    'SYSTEM', '2015-06-15 09:00:00', 'admin', '2021-12-31 17:00:00', 8
);

-- Account 18: Business debit, very high volume
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00002002003', 'GLOBAL VENTURES', '2', 'Y',
    120000.00, 0.00, 0.00,
    '2016-03-22', '2026-03-22', '2024-03-22',
    75000.00, 50000.00,
    '10022', 'NY', 'USA',
    '777 Finance Avenue', 'Floor 15',
    '+12125562001', '+12125562002',
    'GRP004', 'N', 'N',
    'SYSTEM', '2016-03-22 07:30:00', 'ops', '2024-11-01 08:00:00', 30
);

-- =============================================================================
-- BRANCH 00003 - MULTI-STATE ACCOUNTS
-- =============================================================================

-- Account 19: California credit
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00003001001', 'PETER NGUYEN', '1', 'Y',
    2200.00, 7500.00, 3000.00,
    '2022-05-10', '2027-05-10', '2025-05-10',
    1100.00, 800.00,
    '90210', 'CA', 'USA',
    '1 Rodeo Drive', NULL,
    '+13105563001', '+13105563002',
    'GRP005', 'N', 'N',
    'SYSTEM', '2022-05-10 10:00:00', 'admin', '2024-05-10 12:00:00', 4
);

-- Account 20: Texas debit
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00003001002', 'QUINN HARRIS', '2', 'Y',
    6700.00, 0.00, 0.00,
    '2021-08-25', '2026-08-25', '2024-08-25',
    3500.00, 2000.00,
    '75201', 'TX', 'USA',
    '2 Houston Street', 'Apt 301',
    '+12145564001', NULL,
    'GRP005', 'N', 'N',
    'SYSTEM', '2021-08-25 09:00:00', 'ops', '2024-08-25 11:00:00', 6
);

-- Account 21: Florida credit, over limit
-- COBOL: OVER-LIMIT-IND = 'Y'
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00003001003', 'ROSE CLARK', '1', 'Y',
    8100.00, 8000.00, 3500.00,
    '2020-12-01', '2025-12-01', '2023-12-01',
    4500.00, 4600.00,
    '33101', 'FL', 'USA',
    '3 Ocean Drive', 'Suite 200',
    '+13055565001', '+13055565002',
    'GRP005', 'N', 'Y',
    'SYSTEM', '2020-12-01 08:00:00', 'admin', '2024-12-01 10:00:00', 11
);

-- =============================================================================
-- BRANCH 00004 - EXPIRY SCENARIO ACCOUNTS
-- H2 compatible date arithmetic using DATEADD
-- =============================================================================

-- Account 22: Expiring soon (within 3 months)
-- COBOL: EDIT-EXPIRY-DATE scenario testing
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00004001001', 'SAM LEWIS', '1', 'Y',
    1800.00, 4000.00, 1500.00,
    '2019-06-01',
    DATEADD('MONTH', 3, CURRENT_DATE),
    DATEADD('MONTH', 2, CURRENT_DATE),
    900.00, 700.00,
    '30301', 'GA', 'USA',
    '4 Peachtree Street', NULL,
    '+14045566001', NULL,
    'GRP006', 'N', 'N',
    'SYSTEM', '2019-06-01 09:00:00', 'admin', '2024-09-01 10:00:00', 7
);

-- Account 23: Far future expiry
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00004001002', 'TINA WALKER', '1', 'Y',
    500.00, 5000.00, 2000.00,
    '2024-01-15', '2034-01-15', '2032-01-15',
    200.00, 100.00,
    '30302', 'GA', 'USA',
    '5 Auburn Avenue', 'Apt 7',
    '+14045567001', '+14045567002',
    'GRP006', 'N', 'N',
    'admin', '2024-01-15 11:00:00', NULL, NULL, 0
);

-- =============================================================================
-- BRANCH 00005 - INTERNATIONAL ACCOUNTS
-- =============================================================================

-- Account 24: Canadian
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00005001001', 'URSULA KING', '1', 'Y',
    2900.00, 6000.00, 2500.00,
    '2022-03-01', '2027-03-01', '2025-03-01',
    1400.00, 600.00,
    'M5H2N2', 'ON', 'CAN',
    '100 King Street West', 'Suite 1200',
    '+14165568001', '+14165568002',
    'GRP007', 'N', 'N',
    'SYSTEM', '2022-03-01 09:00:00', 'admin', '2024-03-01 10:00:00', 5
);

-- Account 25: UK
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00005001002', 'VICTOR SCOTT', '1', 'Y',
    4100.00, 9000.00, 4000.00,
    '2021-07-04', '2026-07-04', '2024-07-04',
    2100.00, 1100.00,
    'EC2V8RT', 'ENG', 'GBR',
    '1 Lombard Street', '3rd Floor',
    '+44207569001', '+44207569002',
    'GRP007', 'N', 'N',
    'SYSTEM', '2021-07-04 08:00:00', 'admin', '2024-07-04 09:00:00', 7
);

-- Account 26: Australian, INACTIVE
-- COBOL: ACCT-ACTIVE-STATUS = 'N'
INSERT INTO accounts (
    account_id, account_name, account_type, active_status,
    curr_bal, credit_limit, cash_credit_limit,
    open_date, expiry_date, reissue_date,
    curr_cycle_credit, curr_cycle_debit,
    addr_zip, addr_state, addr_country,
    addr_line1, addr_line2,
    phone_number_1, phone_number_2,
    group_id, student_ind, over_limit_ind,
    created_by, created_at, updated_by, updated_at, version
) VALUES (
    '00005001003', 'WENDY HILL', '2', 'N',
    0.00, 0.00, 0.00,
    '2018-11-11', '2023-11-11', '2021-11-11',
    0.00, 0.00,
    '2000', 'NSW', 'AUS',
    '1 Martin Place', NULL,
    '+61295570001', NULL,
    'GRP007', 'N', 'N',
    'SYSTEM', '2018-11-11 07:00:00', 'admin', '2023-12-01 09:00:00', 4
);