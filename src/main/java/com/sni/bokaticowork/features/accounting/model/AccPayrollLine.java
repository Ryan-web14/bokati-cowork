package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Ligne d'un journal de paie importe (lot F17). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_payroll_line")
public class AccPayrollLine {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "import_id", nullable = false)
    private Long importId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "matricule", nullable = false)
    private String matricule;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "rubric_code", nullable = false)
    private String rubricCode;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "section_code")
    private String sectionCode;
}
