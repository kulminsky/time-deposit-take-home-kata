package org.ikigaidigital.domain.interest;

/**
 * Premium plan: 5% a year, interest starts after 45 days.
 */
public final class PremiumPlanInterestRule extends AnnualRateInterestRule {

    public static final String PLAN_TYPE = "premium";

    private static final double ANNUAL_RATE = 0.05;

    // "Interest starts after 45 days": day 46 is the first day with interest (legacy "days > 45").
    private static final int INTEREST_STARTS_AFTER_DAYS = 45;

    public PremiumPlanInterestRule() {
        super(PLAN_TYPE, ANNUAL_RATE);
    }

    @Override
    protected boolean earnsInterest(int days) {
        // The 45-day start already covers the shared 30-day rule; both are kept so the shared rule stays visible.
        return days > ExistingPlanTerms.INTEREST_FREE_DAYS && days > INTEREST_STARTS_AFTER_DAYS;
    }
}
