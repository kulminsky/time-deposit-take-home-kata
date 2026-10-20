package org.ikigaidigital.application.port.out;

import org.ikigaidigital.domain.Withdrawal;

import java.util.List;

/**
 * Storage of withdrawals.
 */
public interface WithdrawalRepository {

    /**
     * All withdrawals of all time deposits, oldest first (by date, then id).
     */
    List<Withdrawal> findAll();
}
