package org.ikigaidigital.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Money taken out of a time deposit.
 * <p>
 * Assumption: withdrawals are history only. The README gives no rule that links them to interest, and the
 * legacy calculator ignores them, so they never change a balance or the interest.
 */
public record Withdrawal(int id, int timeDepositId, BigDecimal amount, LocalDate date) {
}
