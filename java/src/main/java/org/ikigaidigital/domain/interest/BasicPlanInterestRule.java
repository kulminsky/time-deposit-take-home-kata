package org.ikigaidigital.domain.interest;

/**
 * Basic plan: 1% a year.
 */
public final class BasicPlanInterestRule extends AnnualRateInterestRule {

    public static final String PLAN_TYPE = "basic";

    private static final double ANNUAL_RATE = 0.01;

    public BasicPlanInterestRule() {
        super(PLAN_TYPE, ANNUAL_RATE);
    }

    @Override
    protected boolean earnsInterest(int days) {
        return days > ExistingPlanTerms.INTEREST_FREE_DAYS;
    }
}
