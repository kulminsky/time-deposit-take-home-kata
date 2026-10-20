package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Repository
class TimeDepositPersistenceAdapter implements TimeDepositRepository {

    private final TimeDepositJpaRepository jpaRepository;

    TimeDepositPersistenceAdapter(TimeDepositJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<TimeDeposit> findAll() {
        return jpaRepository.findAllByOrderByIdAsc().stream().map(TimeDepositPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<TimeDeposit> findAllForUpdate() {
        return jpaRepository.findAllForUpdate().stream().map(TimeDepositPersistenceAdapter::toDomain).toList();
    }

    // Joins the caller's transaction, so the entities locked by findAllForUpdate() are already managed:
    // getReferenceById returns them without a new query, and Hibernate writes the changed balances on commit.
    @Override
    @Transactional
    public void updateBalances(List<TimeDeposit> timeDeposits) {
        for (TimeDeposit timeDeposit : timeDeposits) {
            jpaRepository.getReferenceById(timeDeposit.getId()).setBalance(toDecimal(timeDeposit.getBalance()));
        }
    }

    // The legacy TimeDeposit holds the balance as a double, while the column is NUMERIC(19,2).
    // Assumption: balances stay below 10^13, so every stored value converts to a double and back exactly.
    private static TimeDeposit toDomain(TimeDepositEntity entity) {
        return new TimeDeposit(entity.getId(), entity.getPlanType(), entity.getBalance().doubleValue(), entity.getDays());
    }

    // BigDecimal.valueOf uses the shortest decimal form of the double (1000.83, not 1000.8299999999999...),
    // and the balance is stored with 2 decimals. The calculator only ever adds whole cents, so this rounding
    // only removes double noise such as 1001.6600000000001; it never moves a balance by a cent.
    private static BigDecimal toDecimal(double balance) {
        return BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP);
    }
}
