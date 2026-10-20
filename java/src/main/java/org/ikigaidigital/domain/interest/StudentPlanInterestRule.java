package org.ikigaidigital.domain.interest;

/**
 * Student plan: 3% a year, no interest after one year.
 */
public final class StudentPlanInterestRule extends AnnualRateInterestRule {

    public static final String PLAN_TYPE = "student";

    private static final double ANNUAL_RATE = 0.03;

    // Assumption: "no interest after 1 year" means one year = 365 days, so day 366 and later earn
    // nothing. This is the legacy "days < 366" check.
    private static final int NO_INTEREST_FROM_DAYS = 366;

    public StudentPlanInterestRule() {
        super(PLAN_TYPE, ANNUAL_RATE);
    }

    @Override
    protected boolean earnsInterest(int days) {
        return days > ExistingPlanTerms.INTEREST_FREE_DAYS && days < NO_INTEREST_FROM_DAYS;
    }
}
