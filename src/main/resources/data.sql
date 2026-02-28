INSERT INTO credit_cards (
    id, card_number, account_id, cvv_code, embossed_name,
    expiration_date, active_status, credit_limit, cash_credit_limit,
    current_balance, current_cycle_credit, current_cycle_debit, group_id, created_at, updated_at, version
) VALUES
(
    1, '4111111111111111', '00000001001', '123', 'JOHN DOE',
    '2028-12-31', 'Y', 5000.00, 1500.00,
    1250.75, 500.00, 250.25, 'PREMIUM', CURRENT_TIMESTAMP, NULL, 0
),
(
    2, '5500000000000004', '00000001002', '456', 'JANE SMITH',
    '2027-06-30', 'Y', 10000.00, 3000.00,
    3500.00, 1000.00, 750.00, 'GOLD', CURRENT_TIMESTAMP, NULL, 0
),
(
    3, '4012888888881881', '00000001003', '789', 'BOB JOHNSON',
    '2026-09-30', 'N', 2500.00, 500.00,
    0.00, 0.00, 0.00, 'STANDARD', CURRENT_TIMESTAMP, NULL, 0
);