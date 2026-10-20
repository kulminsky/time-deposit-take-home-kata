-- Rows are inserted out of id order on purpose, to check that reads sort by id.
INSERT INTO "timeDeposits" ("id", "planType", "days", "balance")
VALUES (3, 'student', 366, 1000.00),
       (1, 'basic', 31, 1000.00),
       (5, 'premium', 46, 1000.00),
       (2, 'student', 365, 1000.00),
       (4, 'premium', 45, 1000.00);

INSERT INTO "withdrawals" ("id", "timeDepositId", "amount", "date")
VALUES (2, 1, 100.00, '2026-02-01'),
       (1, 1, 50.25, '2026-01-15'),
       (3, 5, 200.00, '2026-03-01'),
       (4, 1, 10.00, '2026-02-01');
