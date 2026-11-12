package com.sni.bokaticowork.features.accounting.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Correspondance d'une rubrique de paie vers ses comptes (lot F17). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_payroll_rubric")
public class AccPayrollRubric {
    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "account")
    private String account;

    @Column(name = "counter_account")
    private String counterAccount;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;
}
