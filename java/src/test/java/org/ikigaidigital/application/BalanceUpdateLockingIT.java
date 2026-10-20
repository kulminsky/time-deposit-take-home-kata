package org.ikigaidigital.application;

import org.ikigaidigital.TestcontainersConfiguration;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Balance updates lock the time deposit rows, so two updates run one after the other and neither is lost.
 * The tests commit real transactions, so they are not wrapped in a test transaction.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class BalanceUpdateLockingIT {

    private static final String LOCK_NOT_AVAILABLE = "55P03";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private UpdateAllBalancesUseCase updateAllBalances;

    @Autowired
    private TimeDepositRepository timeDepositRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @BeforeEach
    void insertCommittedTimeDeposits() {
        jdbcTemplate.update("DELETE FROM \"withdrawals\"");
        jdbcTemplate.update("DELETE FROM \"timeDeposits\"");
        jdbcTemplate.update("""
            INSERT INTO "timeDeposits" ("id", "planType", "days", "balance")
            VALUES (1, 'basic', 31, 1000.00), (2, 'student', 365, 1000.00), (3, 'premium', 46, 1000.00)""");
    }

    @AfterEach
    void stopThreads() {
        executor.shutdownNow();
    }

    @Test
    void lockingReadHoldsRowLocksUntilTheTransactionEnds() {
        transactionTemplate.executeWithoutResult(status -> {
            timeDepositRepository.findAllForUpdate();

            assertThatExceptionOfType(SQLException.class)
                .isThrownBy(this::lockRowFromAnotherConnection)
                .satisfies(e -> assertThat(e.getSQLState()).isEqualTo(LOCK_NOT_AVAILABLE));
        });

        assertThat(this.<Boolean>run(this::lockRowFromAnotherConnection)).isTrue();
    }

    @Test
    void plainReadTakesNoRowLocks() {
        transactionTemplate.executeWithoutResult(status -> {
            timeDepositRepository.findAll();

            assertThat(this.<Boolean>run(this::lockRowFromAnotherConnection)).isTrue();
        });
    }

    @Test
    void updateWaitsUntilTheRowsAreUnlocked() throws Exception {
        CountDownLatch rowsLocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> lockHolder = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            timeDepositRepository.findAllForUpdate();
            rowsLocked.countDown();
            await(release);
        }));
        assertThat(rowsLocked.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isTrue();

        Future<?> update = executor.submit(updateAllBalances::updateAllBalances);

        waitUntilASessionWaitsForARowLock();
        assertThat(update).isNotDone();
        assertThat(balance(1)).isEqualByComparingTo("1000.00");

        release.countDown();
        lockHolder.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        update.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        assertThat(balance(1)).isEqualByComparingTo("1000.83");
    }

    @Test
    void twoConcurrentUpdatesAddTwoMonths() throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        Runnable update = () -> {
            await(start);
            updateAllBalances.updateAllBalances();
        };

        Future<?> first = executor.submit(update);
        Future<?> second = executor.submit(update);
        first.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        second.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);

        assertThat(balance(1)).isEqualByComparingTo("1001.66"); // 1000.00 -> 1000.83 -> 1001.66
        assertThat(balance(2)).isEqualByComparingTo("1005.01"); // 1000.00 -> 1002.50 -> 1005.01
        assertThat(balance(3)).isEqualByComparingTo("1008.35"); // 1000.00 -> 1004.17 -> 1008.35
    }

    // Tries to lock deposit 1 from a separate connection without waiting; true if it got the lock.
    private boolean lockRowFromAnotherConnection() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                statement.execute("SELECT \"id\" FROM \"timeDeposits\" WHERE \"id\" = 1 FOR UPDATE NOWAIT");
                return true;
            } finally {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        }
    }

    private void waitUntilASessionWaitsForARowLock() throws InterruptedException {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            Integer waiting = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND query ILIKE '%for no key update%'",
                Integer.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("No database session waited for a row lock within " + TIMEOUT);
    }

    private BigDecimal balance(int id) {
        return jdbcTemplate.queryForObject("SELECT \"balance\" FROM \"timeDeposits\" WHERE \"id\" = ?", BigDecimal.class, id);
    }

    private <T> T run(SqlCall<T> call) {
        try {
            return call.call();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T call() throws SQLException;
    }
}
