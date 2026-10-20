package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The interest rules that apply, looked up by plan type.
 */
public final class InterestRules {

    private final Map<String, InterestRule> rulesByPlanType;

    public InterestRules(Collection<? extends InterestRule> rules) {
        Map<String, InterestRule> byPlanType = new HashMap<>();
        for (InterestRule rule : rules) {
            if (byPlanType.putIfAbsent(rule.planType(), rule) != null) {
                throw new IllegalArgumentException("More than one interest rule for plan type '" + rule.planType() + "'");
            }
        }
        // A HashMap (not Map.copyOf) so that a lookup with a null plan type returns no rule instead of throwing.
        this.rulesByPlanType = Collections.unmodifiableMap(byPlanType);
    }

    /**
     * The rules of the plans offered today: basic, student and premium.
     */
    public static InterestRules defaults() {
        return new InterestRules(List.of(
            new BasicPlanInterestRule(),
            new StudentPlanInterestRule(),
            new PremiumPlanInterestRule()));
    }

    /**
     * Interest for one month, not rounded.
     */
    public double monthlyInterest(TimeDeposit deposit) {
        // Assumption: plan types are matched exactly and case-sensitively, like the legacy equals(...) checks,
        // and a plan type without a rule ("Basic", "gold", ...) earns no interest, as before.
        // A null plan type is invalid input (the README makes planType required, and the timeDeposits column is
        // NOT NULL). It gets no interest here, while the legacy code threw a NullPointerException for it once the
        // deposit was older than 30 days and left the rest of the list unchanged.
        InterestRule rule = rulesByPlanType.get(deposit.getPlanType());
        return rule == null ? 0 : rule.monthlyInterest(deposit);
    }
}
