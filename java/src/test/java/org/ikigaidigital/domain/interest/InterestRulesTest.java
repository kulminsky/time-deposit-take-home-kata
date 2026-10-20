package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InterestRulesTest {

    private final InterestRules defaults = InterestRules.defaults();

    @Test
    void defaultsUseTheRuleOfTheDepositsPlanType() {
        assertThat(defaults.monthlyInterest(new TimeDeposit(1, "basic", 1200.00, 100))).isEqualTo(1.0);
        assertThat(defaults.monthlyInterest(new TimeDeposit(2, "student", 1200.00, 100))).isEqualTo(3.0);
        assertThat(defaults.monthlyInterest(new TimeDeposit(3, "premium", 1200.00, 100))).isEqualTo(5.0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic", "PREMIUM", "gold", "", " student"})
    void planTypeWithoutRuleEarnsNothing(String planType) {
        assertThat(defaults.monthlyInterest(new TimeDeposit(1, planType, 1200.00, 100))).isZero();
    }

    @Test
    void nullPlanTypeEarnsNothing() {
        assertThat(defaults.monthlyInterest(new TimeDeposit(1, null, 1200.00, 100))).isZero();
    }

    @Test
    void newPlanCanBeAddedWithoutChangingTheCalculator() {
        InterestRule gold = new AnnualRateInterestRule("gold", 0.12) {
            @Override
            protected boolean earnsInterest(int days) {
                return days > 0;
            }
        };
        List<InterestRule> rules = List.of(new BasicPlanInterestRule(), gold);
        TimeDepositCalculator calculator = new TimeDepositCalculator(new InterestRules(rules));
        List<TimeDeposit> deposits = List.of(
            new TimeDeposit(1, "gold", 1000.00, 1),
            new TimeDeposit(2, "basic", 1000.00, 31));

        calculator.updateBalance(deposits);

        assertThat(deposits).extracting(TimeDeposit::getBalance).containsExactly(1010.0, 1000.83);
    }

    @Test
    void rejectsTwoRulesForTheSamePlanType() {
        List<InterestRule> rules = List.of(new BasicPlanInterestRule(), new BasicPlanInterestRule());

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new InterestRules(rules))
            .withMessageContaining("basic");
    }
}
