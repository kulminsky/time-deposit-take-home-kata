package org.ikigaidigital;

import org.ikigaidigital.domain.interest.InterestRules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Adds one month of interest to the balance of each time deposit. The interest of each plan type
 * comes from {@link InterestRules}; this class only applies it and rounds it to cents.
 */
public class TimeDepositCalculator {

    private final InterestRules interestRules;

    public TimeDepositCalculator() {
        this(InterestRules.defaults());
    }

    public TimeDepositCalculator(InterestRules interestRules) {
        this.interestRules = interestRules;
    }

    public void updateBalance(List<TimeDeposit> xs) {
        for (TimeDeposit deposit : xs) {
            double interest = interestRules.monthlyInterest(deposit);
            deposit.setBalance(deposit.getBalance() + roundToCents(interest));
        }
    }

    // Kept exactly as in the legacy code: new BigDecimal(double) rounds the exact binary value of the
    // interest, and the result is added to the double balance. BigDecimal.valueOf(double) would round
    // the decimal string instead and give other cents at .xx5 edges (see TimeDepositCalculatorTest).
    private static double roundToCents(double amount) {
        return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
