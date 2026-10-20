package org.ikigaidigital.application.port.in;

/**
 * Adds one month of interest to the balance of every time deposit.
 * <p>
 * Not idempotent: every call adds another month. Only balances change; the days of a deposit stay as they are.
 */
public interface UpdateAllBalancesUseCase {

    void updateAllBalances();
}
