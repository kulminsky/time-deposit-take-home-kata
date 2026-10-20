package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

// Read-only here: timeDepositId is mapped as a plain column (no @ManyToOne), because withdrawals are only
// listed next to their deposit and never navigated from it.
@Entity
@Table(name = "withdrawals")
class WithdrawalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private int timeDepositId;

    private BigDecimal amount;

    private LocalDate date;

    protected WithdrawalEntity() {
        // for JPA
    }

    Integer getId() {
        return id;
    }

    int getTimeDepositId() {
        return timeDepositId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    LocalDate getDate() {
        return date;
    }
}
