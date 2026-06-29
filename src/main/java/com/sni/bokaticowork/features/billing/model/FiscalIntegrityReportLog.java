package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fiscal_integrity_report")
public class FiscalIntegrityReportLog {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "valid", nullable = false)
    private Boolean valid;

    @Column(name = "checked_invoices", nullable = false)
    private Integer checkedInvoices;

    @Column(name = "broken_chains", nullable = false)
    private Integer brokenChains;

    @Column(name = "missing_signatures", nullable = false)
    private Integer missingSignatures;

    @Column(name = "numbering_gaps", nullable = false)
    private Integer numberingGaps;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @Column(name = "triggered_by", nullable = false, length = 40)
    private String triggeredBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
