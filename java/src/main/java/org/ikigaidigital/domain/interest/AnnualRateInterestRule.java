package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;

/**
 * Rule that pays a fixed annual rate, one twelfth per month, while the deposit's days allow it.
 */
public abstract class AnnualRateInterestRule implements InterestRule {

    private static final int MONTHS_PER_YEAR = 12;

    private final String planType;
    private final double annualRate;

    protected AnnualRateInterestRule(String planType, double annualRate) {
        this.planType = planType;
        this.annualRate = annualRate;
    }

    @Override
    public String planType() {
        return planType;
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        if (!earnsInterest(deposit.getDays())) {
            return 0;
        }
        // Same double expression and evaluation order as the legacy calculator (balance * rate / 12),
        // so the result stays bit-for-bit the same. Do not switch to BigDecimal or to balance * (rate / 12).
        return deposit.getBalance() * annualRate / MONTHS_PER_YEAR;
    }

    /**
     * Whether a deposit with this many days earns interest this month.
     */
    protected abstract boolean earnsInterest(int days);
}
