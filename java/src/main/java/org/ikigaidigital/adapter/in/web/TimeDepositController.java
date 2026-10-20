package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.adapter.in.web.api.TimeDepositsApi;
import org.ikigaidigital.adapter.in.web.model.TimeDepositDto;
import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The two REST endpoints. Paths, methods and payloads come from the OpenAPI contract
 * (src/main/resources/static/openapi/time-deposits.yaml) through the generated {@link TimeDepositsApi}.
 */
@RestController
class TimeDepositController implements TimeDepositsApi {

    private final ListTimeDepositsUseCase listTimeDeposits;
    private final UpdateAllBalancesUseCase updateAllBalances;

    TimeDepositController(ListTimeDepositsUseCase listTimeDeposits, UpdateAllBalancesUseCase updateAllBalances) {
        this.listTimeDeposits = listTimeDeposits;
        this.updateAllBalances = updateAllBalances;
    }

    @Override
    public ResponseEntity<List<TimeDepositDto>> listTimeDeposits() {
        return ResponseEntity.ok(listTimeDeposits.listTimeDeposits().stream().map(TimeDepositDtoMapper::toDto).toList());
    }

    // 204 without a body: the new balances are read with GET /time-deposits.
    @Override
    public ResponseEntity<Void> updateAllBalances() {
        updateAllBalances.updateAllBalances();
        return ResponseEntity.noContent().build();
    }
}
