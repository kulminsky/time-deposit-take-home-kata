package org.ikigaidigital.application.service;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class UpdateAllBalancesService implements UpdateAllBalancesUseCase {

    private final TimeDepositRepository timeDepositRepository;
    private final TimeDepositCalculator calculator;

    public UpdateAllBalancesService(TimeDepositRepository timeDepositRepository, TimeDepositCalculator calculator) {
        this.timeDepositRepository = timeDepositRepository;
        this.calculator = calculator;
    }

    // One transaction for all deposits: the rows stay locked from reading to commit, so a second update
    // waits and then adds its month on top of the committed balances (no lost update).
    // Assumption: all deposits fit in memory and the update is short, so there is no lock timeout and no paging.
    @Override
    @Transactional
    public void updateAllBalances() {
        List<TimeDeposit> timeDeposits = timeDepositRepository.findAllForUpdate();
        calculator.updateBalance(timeDeposits);
        timeDepositRepository.updateBalances(timeDeposits);
    }
}
