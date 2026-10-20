package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.adapter.in.web.model.TimeDepositDto;
import org.ikigaidigital.adapter.in.web.model.WithdrawalDto;
import org.ikigaidigital.domain.TimeDepositWithWithdrawals;
import org.ikigaidigital.domain.Withdrawal;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class TimeDepositDtoMapper {

    private TimeDepositDtoMapper() {
    }

    static TimeDepositDto toDto(TimeDepositWithWithdrawals item) {
        TimeDeposit timeDeposit = item.timeDeposit();
        return new TimeDepositDto()
            .id(timeDeposit.getId())
            .planType(timeDeposit.getPlanType())
            .balance(toMoney(timeDeposit.getBalance()))
            .days(timeDeposit.getDays())
            .withdrawals(item.withdrawals().stream().map(TimeDepositDtoMapper::toDto).toList());
    }

    private static WithdrawalDto toDto(Withdrawal withdrawal) {
        return new WithdrawalDto()
            .id(withdrawal.id())
            .amount(withdrawal.amount())
            .date(withdrawal.date());
    }

    // The legacy TimeDeposit holds the balance as a double; the API shows it as a decimal with 2 places
    // (1000.83, 1000.00), the same as it is stored.
    private static BigDecimal toMoney(double balance) {
        return BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP);
    }
}
