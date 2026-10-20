package org.ikigaidigital.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface WithdrawalJpaRepository extends JpaRepository<WithdrawalEntity, Integer> {

    // Assumption: withdrawals are listed oldest first; the id breaks ties on the same date.
    List<WithdrawalEntity> findAllByOrderByDateAscIdAsc();
}
