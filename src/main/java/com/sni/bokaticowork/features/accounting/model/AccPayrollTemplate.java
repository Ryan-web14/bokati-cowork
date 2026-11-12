package com.sni.bokaticowork.features.accounting.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Modele d'import d'un journal de paie (lot F17). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_payroll_template")
public class AccPayrollTemplate {
    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Builder.Default
    @Column(name = "separator", nullable = false)
    private String separator = ";";

    @Builder.Default
    @Column(name = "header_rows", nullable = false)
    private Integer headerRows = 1;

    @Column(name = "employee_column", nullable = false)
    private String employeeColumn;

    @Column(name = "name_column")
    private String nameColumn;

    @Column(name = "rubric_column", nullable = false)
    private String rubricColumn;

    @Column(name = "amount_column", nullable = false)
    private String amountColumn;

    @Column(name = "section_column")
    private String sectionColumn;

    @Builder.Default
    @Column(name = "decimal_separator", nullable = false)
    private String decimalSeparator = ",";

    @Builder.Default
    @Column(name = "encoding", nullable = false)
    private String encoding = "UTF-8";

    @Builder.Default
    @Column(name = "created_by", nullable = false)
    private String createdBy = "SYSTEM";
}
