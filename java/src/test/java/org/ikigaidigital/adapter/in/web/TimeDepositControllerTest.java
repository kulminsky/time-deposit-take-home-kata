package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.domain.TimeDepositWithWithdrawals;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@WebMvcTest(TimeDepositController.class)
class TimeDepositControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListTimeDepositsUseCase listTimeDeposits;

    @MockitoBean
    private UpdateAllBalancesUseCase updateAllBalances;

    @Test
    void listsTimeDepositsWithWithdrawals() {
        given(listTimeDeposits.listTimeDeposits()).willReturn(List.of(
            new TimeDepositWithWithdrawals(new TimeDeposit(1, "basic", 1000.83, 31), List.of(
                new Withdrawal(7, 1, new BigDecimal("50.25"), LocalDate.of(2026, 1, 15)))),
            new TimeDepositWithWithdrawals(new TimeDeposit(2, "premium", 1000.0, 45), List.of())));

        assertThat(mvc.get().uri("/time-deposits"))
            .hasStatusOk()
            .bodyJson().isStrictlyEqualTo("""
                [
                  {"id": 1, "planType": "basic", "balance": 1000.83, "days": 31,
                   "withdrawals": [{"id": 7, "amount": 50.25, "date": "2026-01-15"}]},
                  {"id": 2, "planType": "premium", "balance": 1000.00, "days": 45, "withdrawals": []}
                ]
                """);
        verify(updateAllBalances, never()).updateAllBalances();
    }

    @Test
    void showsBalancesWithTwoDecimals() {
        given(listTimeDeposits.listTimeDeposits()).willReturn(List.of(
            new TimeDepositWithWithdrawals(new TimeDeposit(1, "basic", 1000.0, 31), List.of())));

        assertThat(mvc.get().uri("/time-deposits"))
            .hasStatusOk()
            .bodyText().contains("\"balance\":1000.00");
    }

    @Test
    void updatesAllBalancesAndReturnsNoContent() {
        assertThat(mvc.post().uri("/time-deposits/balance-updates"))
            .hasStatus(HttpStatus.NO_CONTENT)
            .body().isEmpty();
        verify(updateAllBalances).updateAllBalances();
    }

    @Test
    void doesNotUpdateBalancesWithPut() {
        assertThat(mvc.put().uri("/time-deposits/balance-updates")).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
        verify(updateAllBalances, never()).updateAllBalances();
    }
}
