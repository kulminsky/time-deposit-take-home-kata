package org.ikigaidigital.application.service;

import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.application.port.out.WithdrawalRepository;
import org.ikigaidigital.domain.TimeDepositWithWithdrawals;
import org.ikigaidigital.domain.Withdrawal;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListTimeDepositsService implements ListTimeDepositsUseCase {

    private final TimeDepositRepository timeDepositRepository;
    private final WithdrawalRepository withdrawalRepository;

    public ListTimeDepositsService(TimeDepositRepository timeDepositRepository, WithdrawalRepository withdrawalRepository) {
        this.timeDepositRepository = timeDepositRepository;
        this.withdrawalRepository = withdrawalRepository;
    }

    // Two queries (deposits, withdrawals) joined in memory instead of one query per deposit.
    // Assumption: all deposits and their withdrawals fit in memory, so the list is not paged.
    // Reads never lock, so listing is not blocked by a running balance update; it shows the last committed balances.
    @Override
    @Transactional(readOnly = true)
    public List<TimeDepositWithWithdrawals> listTimeDeposits() {
        Map<Integer, List<Withdrawal>> withdrawalsByTimeDepositId = withdrawalRepository.findAll().stream()
            .collect(Collectors.groupingBy(Withdrawal::timeDepositId));
        return timeDepositRepository.findAll().stream()
            .map(timeDeposit -> new TimeDepositWithWithdrawals(
                timeDeposit, withdrawalsByTimeDepositId.getOrDefault(timeDeposit.getId(), List.of())))
            .toList();
    }
}
