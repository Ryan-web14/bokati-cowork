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

/** Journal de paie importe pour un mois (lot F17). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_payroll_import")
public class AccPayrollImport {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "period_code", nullable = false)
    private String periodCode;

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Column(name = "file_name")
    private String fileName;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "DRAFT";

    @Builder.Default
    @Column(name = "gross_total", nullable = false)
    private BigDecimal grossTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "employer_total", nullable = false)
    private BigDecimal employerTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "deduction_total", nullable = false)
    private BigDecimal deductionTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "advance_total", nullable = false)
    private BigDecimal advanceTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "net_total", nullable = false)
    private BigDecimal netTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "paid_total", nullable = false)
    private BigDecimal paidTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "employee_count", nullable = false)
    private Integer employeeCount = 0;

    @Column(name = "errors_json")
    private String errorsJson;

    @Column(name = "warnings_json")
    private String warningsJson;

    @Column(name = "entry_number")
    private String entryNumber;

    @Column(name = "imported_by", nullable = false)
    private String importedBy;

    @Builder.Default
    @Column(name = "imported_at", nullable = false)
    private Instant importedAt = Instant.now();

    @Column(name = "posted_by")
    private String postedBy;

    @Column(name = "posted_at")
    private Instant postedAt;
}
