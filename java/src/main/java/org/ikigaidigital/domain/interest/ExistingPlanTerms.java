package org.ikigaidigital.domain.interest;

/**
 * Terms shared by the plans that exist today (basic, student, premium).
 */
final class ExistingPlanTerms {

    // README: "No interest is applied for the first 30 days for any existing plans."
    // Assumption: this belongs to the existing plans only, not to every future plan, so it lives here
    // and each existing plan applies it; a new plan decides its own terms. Day 31 is the first day with interest.
    static final int INTEREST_FREE_DAYS = 30;

    private ExistingPlanTerms() {
    }
}
