package org.ikigaidigital.application.port.in;

import org.ikigaidigital.domain.TimeDepositWithWithdrawals;

import java.util.List;

/**
 * Lists all time deposits with their withdrawals.
 */
public interface ListTimeDepositsUseCase {

    List<TimeDepositWithWithdrawals> listTimeDeposits();
}
