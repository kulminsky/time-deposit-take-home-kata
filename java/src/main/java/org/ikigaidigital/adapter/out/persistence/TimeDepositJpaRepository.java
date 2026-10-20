package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface TimeDepositJpaRepository extends JpaRepository<TimeDepositEntity, Integer> {

    List<TimeDepositEntity> findAllByOrderByIdAsc();

    // SELECT ... ORDER BY id FOR NO KEY UPDATE on Postgres: each row stays locked for writing until the
    // transaction ends. Locking in id order means two concurrent updates cannot deadlock.
    // Assumption: no lock timeout; a second update simply waits for the first one to commit.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select timeDeposit from TimeDepositEntity timeDeposit order by timeDeposit.id")
    List<TimeDepositEntity> findAllForUpdate();
}
