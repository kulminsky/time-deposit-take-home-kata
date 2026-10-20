package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Characterization tests for {@link TimeDepositCalculator#updateBalance(List)}.
 * Every expected value was taken from running the original (pre-refactoring) calculator
 * and is compared as an exact double. The one exception is the null plan type test, a deliberate change.
 */
public class TimeDepositCalculatorTest {

    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @Test
    public void updateBalance_Test() {
        List<TimeDeposit> plans = Arrays.asList(
            new TimeDeposit(1,"basic", 1234567.00, 45)
        );
        calculator.updateBalance(plans);

        assertThat(plans.get(0).getBalance()).isEqualTo(1235595.81);
    }

    @ParameterizedTest(name = "{0}, {1} days: 1000.00 -> {2}")
    @CsvSource({
        "basic,      0, 1000.0",
        "basic,     30, 1000.0",
        "basic,     31, 1000.83",
        "basic,     45, 1000.83",
        "basic,    366, 1000.83",
        "basic,   1000, 1000.83",
        "student,    0, 1000.0",
        "student,   30, 1000.0",
        "student,   31, 1002.5",
        "student,  365, 1002.5",
        "student,  366, 1000.0",
        "student, 1000, 1000.0",
        "premium,    0, 1000.0",
        "premium,   30, 1000.0",
        "premium,   31, 1000.0",
        "premium,   45, 1000.0",
        "premium,   46, 1004.17",
        "premium,  366, 1004.17",
        "premium, 1000, 1004.17",
    })
    void addsOneMonthOfInterestDependingOnPlanAndDays(String planType, int days, double expectedBalance) {
        assertThat(balanceAfterUpdate(planType, 1000.00, days)).isEqualTo(expectedBalance);
    }

    @ParameterizedTest(name = "plan type \"{0}\" gets no interest")
    @CsvSource({"Basic", "STUDENT", "Premium", "gold", "''", "' basic'"})
    void matchesPlanTypeExactlyAndCaseSensitively(String planType) {
        assertThat(balanceAfterUpdate(planType, 1000.00, 100)).isEqualTo(1000.0);
    }

    // Deliberate change, not a value from the original calculator (see InterestRules): a null plan type gets no
    // interest and the rest of the list is still updated. The original threw a NullPointerException here and
    // left the rest of the list unchanged.
    @Test
    void givesNoInterestToANullPlanTypeAndUpdatesTheRestOfTheList() {
        List<TimeDeposit> deposits = List.of(
            new TimeDeposit(1, null, 1000.00, 100),
            new TimeDeposit(2, "basic", 1000.00, 31));

        calculator.updateBalance(deposits);

        assertThat(deposits).extracting(TimeDeposit::getBalance).containsExactly(1000.0, 1000.83);
    }

    // The interest is rounded from its exact binary double value (new BigDecimal(double)).
    // For these balances the double interest is just below x.xx5, so it rounds down;
    // BigDecimal.valueOf(double) would round up to 18.02, 6.02 and 18.08.
    @ParameterizedTest(name = "{0}, balance {1} -> {2}")
    @CsvSource({
        "basic,   18.00, 18.01",
        "student,  6.00,  6.01",
        "premium, 18.00, 18.07",
    })
    void roundsTheExactDoubleInterestHalfUp(String planType, double balance, double expectedBalance) {
        assertThat(balanceAfterUpdate(planType, balance, 100)).isEqualTo(expectedBalance);
    }

    @Test
    void keepsZeroBalanceAtZero() {
        assertThat(balanceAfterUpdate("basic", 0.0, 100)).isEqualTo(0.0);
    }

    @Test
    void appliesNegativeInterestToNegativeBalance() {
        assertThat(balanceAfterUpdate("basic", -1000.00, 31)).isEqualTo(-1000.83);
        assertThat(balanceAfterUpdate("premium", -1000.00, 46)).isEqualTo(-1004.17);
    }

    @Test
    void updatesEveryDepositOfTheListInPlace() {
        TimeDeposit basic = new TimeDeposit(1, "basic", 1000.00, 31);
        TimeDeposit student = new TimeDeposit(2, "student", 1000.00, 366);
        TimeDeposit premium = new TimeDeposit(3, "premium", 1000.00, 46);
        List<TimeDeposit> deposits = new ArrayList<>(List.of(basic, student, premium));

        calculator.updateBalance(deposits);

        assertThat(deposits).containsExactly(basic, student, premium);
        assertThat(deposits)
            .extracting(TimeDeposit::getId, TimeDeposit::getPlanType, TimeDeposit::getBalance, TimeDeposit::getDays)
            .containsExactly(
                tuple(1, "basic", 1000.83, 31),
                tuple(2, "student", 1000.0, 366),
                tuple(3, "premium", 1004.17, 46));
    }

    @Test
    void addsInterestOnTopOfThePreviousUpdate() {
        List<TimeDeposit> deposits = List.of(new TimeDeposit(1, "premium", 1000.00, 46));

        calculator.updateBalance(deposits);
        calculator.updateBalance(deposits);

        // 1000.00 -> 1004.17 -> 1004.17 + 4.18; the balance is a double, so the sum is not exactly 1008.35.
        assertThat(deposits.get(0).getBalance()).isEqualTo(1008.3499999999999);
    }

    @Test
    void doesNothingForAnEmptyList() {
        List<TimeDeposit> deposits = new ArrayList<>();

        calculator.updateBalance(deposits);

        assertThat(deposits).isEmpty();
    }

    private double balanceAfterUpdate(String planType, double balance, int days) {
        TimeDeposit deposit = new TimeDeposit(1, planType, balance, days);
        calculator.updateBalance(List.of(deposit));
        return deposit.getBalance();
    }
}
