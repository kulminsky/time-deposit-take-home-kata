package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.application.port.out.WithdrawalRepository;
import org.ikigaidigital.domain.Withdrawal;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class WithdrawalPersistenceAdapter implements WithdrawalRepository {

    private final WithdrawalJpaRepository jpaRepository;

    WithdrawalPersistenceAdapter(WithdrawalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Withdrawal> findAll() {
        return jpaRepository.findAllByOrderByDateAscIdAsc().stream()
            .map(entity -> new Withdrawal(entity.getId(), entity.getTimeDepositId(), entity.getAmount(), entity.getDate()))
            .toList();
    }
}
