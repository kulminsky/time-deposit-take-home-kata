package org.ikigaidigital.application.service;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateAllBalancesServiceTest {

    private final InMemoryTimeDepositRepository repository = new InMemoryTimeDepositRepository(
        new TimeDeposit(1, "basic", 1000.00, 31),
        new TimeDeposit(2, "student", 1000.00, 366),
        new TimeDeposit(3, "premium", 1000.00, 46));

    private final UpdateAllBalancesService service = new UpdateAllBalancesService(repository, new TimeDepositCalculator());

    @Test
    void storesOneMonthOfInterestForEveryDeposit() {
        service.updateAllBalances();

        assertThat(repository.balanceOf(1)).isEqualTo(1000.83);
        assertThat(repository.balanceOf(2)).isEqualTo(1000.0);
        assertThat(repository.balanceOf(3)).isEqualTo(1004.17);
    }

    @Test
    void readsTheDepositsWithRowLocks() {
        service.updateAllBalances();

        assertThat(repository.lockingReads()).isEqualTo(1);
        assertThat(repository.plainReads()).isZero();
    }

    @Test
    void addsAnotherMonthOnEveryCall() {
        service.updateAllBalances();
        service.updateAllBalances();

        // The fake keeps the raw double; the database adapter stores the balance with 2 decimals.
        assertThat(repository.balanceOf(1)).isEqualTo(1000.83 + 0.83);
    }
}
