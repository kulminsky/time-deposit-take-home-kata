package org.ikigaidigital.application.port.out;

import org.ikigaidigital.TimeDeposit;

import java.util.List;

/**
 * Storage of time deposits.
 */
public interface TimeDepositRepository {

    /**
     * All time deposits ordered by id, without locking them.
     */
    List<TimeDeposit> findAll();

    /**
     * All time deposits ordered by id, each row locked for writing until the current transaction ends.
     * Must be called inside a transaction. Another caller of this method waits until that transaction
     * commits and then gets the committed balances, so two balance updates never overlap.
     */
    List<TimeDeposit> findAllForUpdate();

    /**
     * Stores the balances of the given deposits. Other fields are not changed.
     */
    void updateBalances(List<TimeDeposit> timeDeposits);
}
