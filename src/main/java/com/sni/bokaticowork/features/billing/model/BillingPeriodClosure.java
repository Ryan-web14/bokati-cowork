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

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_period_closure")
public class BillingPeriodClosure {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "period_type", nullable = false, length = 10)
    private String periodType;

    @Column(name = "period_label", nullable = false, length = 20)
    private String periodLabel;

    @Column(name = "document_type", nullable = false, length = 40)
    private String documentType;

    @Column(name = "total_documents", nullable = false)
    private Integer totalDocuments;

    @Column(name = "total_invoiced", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalInvoiced;

    @Column(name = "total_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalPaid;

    @Column(name = "total_credit_notes", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCreditNotes;

    @Column(name = "cumulative_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal cumulativeTotal;

    @Column(name = "closure_hash", nullable = false, length = 64)
    private String closureHash;

    @Column(name = "previous_closure_hash", length = 64)
    private String previousClosureHash;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @Column(name = "computed_by", nullable = false, length = 120)
    private String computedBy;
}
