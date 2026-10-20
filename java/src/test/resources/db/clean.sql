-- Removes all rows. The integration tests share one database, so each test starts from empty tables.
DELETE FROM "withdrawals";
DELETE FROM "timeDeposits";
