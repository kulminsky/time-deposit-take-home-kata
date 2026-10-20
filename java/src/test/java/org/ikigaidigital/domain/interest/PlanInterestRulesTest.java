package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PlanInterestRulesTest {

    private final InterestRule basic = new BasicPlanInterestRule();
    private final InterestRule student = new StudentPlanInterestRule();
    private final InterestRule premium = new PremiumPlanInterestRule();

    @Test
    void ruleNamesMatchTheLegacyPlanTypes() {
        assertThat(basic.planType()).isEqualTo("basic");
        assertThat(student.planType()).isEqualTo("student");
        assertThat(premium.planType()).isEqualTo("premium");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 30})
    void basicEarnsNothingInTheFirst30Days(int days) {
        assertThat(basic.monthlyInterest(deposit("basic", days))).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 365, 366, 5000})
    void basicEarnsOnePercentAYearFromDay31(int days) {
        assertThat(basic.monthlyInterest(deposit("basic", days))).isEqualTo(1200.00 * 0.01 / 12);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 30, 366, 367, 5000})
    void studentEarnsNothingInTheFirst30DaysOrAfterOneYear(int days) {
        assertThat(student.monthlyInterest(deposit("student", days))).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 200, 365})
    void studentEarnsThreePercentAYearFromDay31ToDay365(int days) {
        assertThat(student.monthlyInterest(deposit("student", days))).isEqualTo(1200.00 * 0.03 / 12);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 30, 31, 45})
    void premiumEarnsNothingInTheFirst45Days(int days) {
        assertThat(premium.monthlyInterest(deposit("premium", days))).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {46, 366, 5000})
    void premiumEarnsFivePercentAYearFromDay46(int days) {
        assertThat(premium.monthlyInterest(deposit("premium", days))).isEqualTo(1200.00 * 0.05 / 12);
    }

    @Test
    void interestIsNotRounded() {
        assertThat(basic.monthlyInterest(new TimeDeposit(1, "basic", 1000.00, 31))).isEqualTo(1000.00 * 0.01 / 12);
        assertThat(basic.monthlyInterest(new TimeDeposit(1, "basic", 1000.00, 31))).isNotEqualTo(0.83);
    }

    private static TimeDeposit deposit(String planType, int days) {
        return new TimeDeposit(1, planType, 1200.00, days);
    }
}
