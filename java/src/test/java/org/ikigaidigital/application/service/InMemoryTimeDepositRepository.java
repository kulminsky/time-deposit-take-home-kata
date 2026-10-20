package org.ikigaidigital.application.service;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.out.TimeDepositRepository;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Fake repository that behaves like a table: reads return copies, so a balance only changes when
 * {@link #updateBalances(List)} stores it.
 */
class InMemoryTimeDepositRepository implements TimeDepositRepository {

    private final Map<Integer, TimeDeposit> rowsById = new TreeMap<>();
    private int lockingReads;
    private int plainReads;

    InMemoryTimeDepositRepository(TimeDeposit... timeDeposits) {
        for (TimeDeposit timeDeposit : timeDeposits) {
            rowsById.put(timeDeposit.getId(), copyOf(timeDeposit));
        }
    }

    @Override
    public List<TimeDeposit> findAll() {
        plainReads++;
        return copies();
    }

    @Override
    public List<TimeDeposit> findAllForUpdate() {
        lockingReads++;
        return copies();
    }

    @Override
    public void updateBalances(List<TimeDeposit> timeDeposits) {
        timeDeposits.forEach(timeDeposit -> rowsById.get(timeDeposit.getId()).setBalance(timeDeposit.getBalance()));
    }

    double balanceOf(int id) {
        return rowsById.get(id).getBalance();
    }

    int lockingReads() {
        return lockingReads;
    }

    int plainReads() {
        return plainReads;
    }

    private List<TimeDeposit> copies() {
        return rowsById.values().stream().map(InMemoryTimeDepositRepository::copyOf).toList();
    }

    private static TimeDeposit copyOf(TimeDeposit timeDeposit) {
        return new TimeDeposit(timeDeposit.getId(), timeDeposit.getPlanType(), timeDeposit.getBalance(), timeDeposit.getDays());
    }
}
