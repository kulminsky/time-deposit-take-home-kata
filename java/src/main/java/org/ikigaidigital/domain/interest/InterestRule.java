package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;

/**
 * Interest rule of one plan type. To support a new plan, implement this interface and register it
 * in {@link InterestRules}; the calculator does not change.
 */
public interface InterestRule {

    /**
     * Plan type this rule is for. It is compared exactly and case-sensitively with
     * {@link TimeDeposit#getPlanType()}.
     */
    String planType();

    /**
     * Interest for one month, not rounded. Rounding to cents is done by the calculator.
     */
    double monthlyInterest(TimeDeposit deposit);
}
