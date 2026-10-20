package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TestcontainersConfiguration;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.groups.Tuple.tuple;

// Runs against Postgres with the Flyway schema; Hibernate's ddl-auto=validate checks the entities on start-up.
// Each test runs in a transaction that is rolled back.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, TimeDepositPersistenceAdapter.class, WithdrawalPersistenceAdapter.class})
@Sql("/db/test-data.sql")
class PersistenceAdapterIT {

    @Autowired
    private TimeDepositPersistenceAdapter timeDeposits;

    @Autowired
    private WithdrawalPersistenceAdapter withdrawals;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsAllTimeDepositsOrderedById() {
        assertThat(timeDeposits.findAll())
            .extracting(TimeDeposit::getId, TimeDeposit::getPlanType, TimeDeposit::getDays, TimeDeposit::getBalance)
            .containsExactly(
                tuple(1, "basic", 31, 1000.0),
                tuple(2, "student", 365, 1000.0),
                tuple(3, "student", 366, 1000.0),
                tuple(4, "premium", 45, 1000.0),
                tuple(5, "premium", 46, 1000.0));
    }

    @Test
    void findsAllTimeDepositsForUpdateOrderedById() {
        assertThat(timeDeposits.findAllForUpdate()).extracting(TimeDeposit::getId).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void storesBalancesWithTwoDecimals() {
        List<TimeDeposit> loaded = timeDeposits.findAllForUpdate();
        loaded.get(0).setBalance(1000.83 + 0.83); // 1001.6600000000001 as a double
        loaded.get(1).setBalance(1002.5);

        timeDeposits.updateBalances(loaded.subList(0, 2));
        entityManager.flush();

        assertThat(balanceInDatabase(1)).isEqualByComparingTo("1001.66");
        assertThat(balanceInDatabase(2)).isEqualByComparingTo("1002.50");
        assertThat(balanceInDatabase(3)).isEqualByComparingTo("1000.00");
    }

    @Test
    void storesTheLargestBalanceAndReadsItBackExactly() {
        List<TimeDeposit> loaded = timeDeposits.findAllForUpdate();
        loaded.get(0).setBalance(9_999_999_999_999.99);

        timeDeposits.updateBalances(loaded.subList(0, 1));
        entityManager.flush();
        entityManager.clear();

        assertThat(balanceInDatabase(1)).isEqualByComparingTo("9999999999999.99");
        assertThat(timeDeposits.findAll().get(0).getBalance()).isEqualTo(9_999_999_999_999.99);
    }

    @Test
    void balanceColumnRejects10ToThe13() {
        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() -> jdbcTemplate.update(
            "UPDATE \"timeDeposits\" SET \"balance\" = 10000000000000.00 WHERE \"id\" = 1"));
    }

    @Test
    void findsAllWithdrawalsOldestFirst() {
        assertThat(withdrawals.findAll()).containsExactly(
            new Withdrawal(1, 1, new BigDecimal("50.25"), LocalDate.of(2026, 1, 15)),
            new Withdrawal(2, 1, new BigDecimal("100.00"), LocalDate.of(2026, 2, 1)),
            new Withdrawal(4, 1, new BigDecimal("10.00"), LocalDate.of(2026, 2, 1)),
            new Withdrawal(3, 5, new BigDecimal("200.00"), LocalDate.of(2026, 3, 1)));
    }

    private BigDecimal balanceInDatabase(int id) {
        return jdbcTemplate.queryForObject("SELECT \"balance\" FROM \"timeDeposits\" WHERE \"id\" = ?", BigDecimal.class, id);
    }
}
