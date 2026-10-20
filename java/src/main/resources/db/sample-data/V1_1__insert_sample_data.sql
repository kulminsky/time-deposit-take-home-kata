-- Sample data for trying the API locally. Only the "local" profile loads this folder (application-local.yaml);
-- tests and other environments start with empty tables.
-- One deposit per interest case; the comments show the balance after one POST /time-deposits/balance-updates.
INSERT INTO "timeDeposits" ("id", "planType", "days", "balance")
VALUES (1, 'basic', 30, 1000.00),      -- 1000.00: no interest in the first 30 days
       (2, 'basic', 31, 1000.00),      -- 1000.83
       (3, 'student', 365, 1000.00),   -- 1002.50
       (4, 'student', 366, 1000.00),   -- 1000.00: no interest after one year
       (5, 'premium', 45, 1000.00),    -- 1000.00: interest starts after 45 days
       (6, 'premium', 46, 1000.00),    -- 1004.17
       (7, 'basic', 120, 1234567.00),  -- 1235595.81
       (8, 'gold', 100, 500.00);       -- 500.00: unknown plan type, no interest

INSERT INTO "withdrawals" ("id", "timeDepositId", "amount", "date")
VALUES (1, 2, 50.00, '2026-01-15'),
       (2, 6, 100.00, '2026-02-01'),
       (3, 6, 25.50, '2026-03-01'),
       (4, 7, 10000.00, '2026-03-10');

-- The ids above were given explicitly; move the identity sequences past them.
SELECT setval(pg_get_serial_sequence('"timeDeposits"', 'id'), (SELECT max("id") FROM "timeDeposits"));
SELECT setval(pg_get_serial_sequence('"withdrawals"', 'id'), (SELECT max("id") FROM "withdrawals"));
