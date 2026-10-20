package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

import java.util.List;

/**
 * Read model: a time deposit together with its withdrawals. {@link TimeDeposit} must not change, so the
 * withdrawals are kept next to it instead of inside it.
 */
public record TimeDepositWithWithdrawals(TimeDeposit timeDeposit, List<Withdrawal> withdrawals) {

    public TimeDepositWithWithdrawals {
        withdrawals = List.copyOf(withdrawals);
    }
}
