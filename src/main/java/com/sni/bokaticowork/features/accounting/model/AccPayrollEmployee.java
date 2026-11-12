package com.sni.bokaticowork.features.accounting.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Salarie de la paie : matricule, tiers comptable, section (lot F17). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_payroll_employee")
public class AccPayrollEmployee {
    @Id
    @Column(name = "matricule")
    private String matricule;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "third_party_code", nullable = false)
    private String thirdPartyCode;

    @Column(name = "section_code")
    private String sectionCode;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;
}
