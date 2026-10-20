package org.ikigaidigital.application.service;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.out.WithdrawalRepository;
import org.ikigaidigital.domain.TimeDepositWithWithdrawals;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListTimeDepositsServiceTest {

    private static final Withdrawal FIRST_OF_1 = new Withdrawal(10, 1, new BigDecimal("50.00"), LocalDate.of(2026, 1, 15));
    private static final Withdrawal SECOND_OF_1 = new Withdrawal(11, 1, new BigDecimal("25.50"), LocalDate.of(2026, 2, 1));
    private static final Withdrawal ONLY_OF_3 = new Withdrawal(12, 3, new BigDecimal("100.00"), LocalDate.of(2026, 3, 9));

    private final InMemoryTimeDepositRepository timeDeposits = new InMemoryTimeDepositRepository(
        new TimeDeposit(1, "basic", 1000.00, 31),
        new TimeDeposit(2, "student", 2000.00, 100),
        new TimeDeposit(3, "premium", 3000.00, 46));
    private final WithdrawalRepository withdrawals = () -> List.of(FIRST_OF_1, SECOND_OF_1, ONLY_OF_3);

    private final ListTimeDepositsService service = new ListTimeDepositsService(timeDeposits, withdrawals);

    @Test
    void listsEveryDepositWithItsOwnWithdrawals() {
        List<TimeDepositWithWithdrawals> result = service.listTimeDeposits();

        assertThat(result).extracting(item -> item.timeDeposit().getId()).containsExactly(1, 2, 3);
        assertThat(result.get(0).withdrawals()).containsExactly(FIRST_OF_1, SECOND_OF_1);
        assertThat(result.get(1).withdrawals()).isEmpty();
        assertThat(result.get(2).withdrawals()).containsExactly(ONLY_OF_3);
    }

    @Test
    void keepsTheDepositFields() {
        TimeDeposit first = service.listTimeDeposits().get(0).timeDeposit();

        assertThat(first.getPlanType()).isEqualTo("basic");
        assertThat(first.getBalance()).isEqualTo(1000.00);
        assertThat(first.getDays()).isEqualTo(31);
    }

    @Test
    void readsWithoutRowLocks() {
        service.listTimeDeposits();

        assertThat(timeDeposits.plainReads()).isEqualTo(1);
        assertThat(timeDeposits.lockingReads()).isZero();
    }
}
