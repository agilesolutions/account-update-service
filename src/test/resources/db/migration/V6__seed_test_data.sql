-- ============================================================
-- Flyway migration V6: seed data for DEV / TEST environments
--
-- Do NOT run in production. Use a Flyway placeholder or profile
-- guard if you share migrations across environments:
--
--   spring.flyway.locations=classpath:db/migration,classpath:db/testdata
--   (put V6 only in db/testdata)
--
-- This data matches the test cases in InterestCalculatorServiceTest
-- so you can run the Java batch against a real PostgreSQL instance
-- and compare output to the COBOL test run.
-- ============================================================

-- -------------------------------------------------------
-- Accounts
-- -------------------------------------------------------
INSERT INTO carddemo.account
    (acct_id, acct_active_status, acct_curr_bal, acct_credit_limit,
     acct_curr_cyc_credit, acct_curr_cyc_debit, acct_group_id)
VALUES
    ('00000000001', 'Y', 5000.00, 10000.00, 0.00, 0.00, 'GRPA      '),
    ('00000000002', 'Y', 1000.00, 10000.00, 0.00, 0.00, 'GRPB      '),
    ('00000000003', 'Y',    0.00,  5000.00, 0.00, 0.00, 'GRP1      '),
    ('00000000004', 'Y',    0.00,  5000.00, 0.00, 0.00, 'GRP1      '),
    ('00000000005', 'Y',    0.00,  5000.00, 0.00, 0.00, 'UNKNOWN   ');

-- -------------------------------------------------------
-- Card cross-reference
-- -------------------------------------------------------
INSERT INTO carddemo.card_xref (card_num, cust_num, acct_id)
VALUES
    ('1234567890123456', 100000001, '00000000001'),
    ('9999888877776666', 100000002, '00000000002'),
    ('1111111111111111', 100000003, '00000000003'),
    ('2222222222222222', 100000004, '00000000004'),
    ('5555555555555555', 100000005, '00000000005');

-- -------------------------------------------------------
-- Disclosure group rates
-- Amounts match the test assertions in InterestCalculatorServiceTest
-- -------------------------------------------------------
INSERT INTO carddemo.disclosure_group
    (acct_group_id, tran_type_cd, tran_cat_cd, dis_int_rate, description)
VALUES
    -- TC-01: 18% APR on GRPA/PR/1 → (1200 * 18) / 1200 = 18.00
    ('GRPA      ', 'PR', 1, 18.0000, 'Group A - Purchases cat 1'),
    -- TC-02: 0% on GRPB/PR/2 → no transaction
    ('GRPB      ', 'PR', 2,  0.0000, 'Group B - Purchases cat 2 (promotional 0%)'),
    -- TC-03/04: 24% APR on GRP1/PR/1 → (600 * 24) / 1200 = 12, (1200 * 24) / 1200 = 24
    ('GRP1      ', 'PR', 1, 24.0000, 'Group 1 - Purchases cat 1'),
    ('GRP1      ', 'CR', 2, 24.0000, 'Group 1 - Credits cat 2'),
    ('GRP1      ', 'PR', 2, 12.0000, 'Group 1 - Purchases cat 2')
ON CONFLICT DO NOTHING;

-- -------------------------------------------------------
-- Transaction category balances (the input file for CBACT04C)
-- -------------------------------------------------------
INSERT INTO carddemo.tran_cat_bal (acct_id, tran_type_cd, tran_cat_cd, tran_cat_bal)
VALUES
    -- TC-01: single account, single category
    ('00000000001', 'PR', 1, 1200.00),
    -- TC-02: zero-rate category
    ('00000000002', 'PR', 2,  500.00),
    -- TC-03: two accounts, same category
    ('00000000003', 'PR', 1, 1200.00),
    ('00000000004', 'PR', 1,  600.00),
    -- TC-04: multiple categories for one account
    --        (reuse acct 3 with a second category — adjust if running TC-03 separately)
    -- ('00000000003', 'CR', 2, 2400.00),
    -- TC-05: unknown group → DEFAULT fallback (18% APR default)
    ('00000000005', 'PR', 1, 1200.00);
