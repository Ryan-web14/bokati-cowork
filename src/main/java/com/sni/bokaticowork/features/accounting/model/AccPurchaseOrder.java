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

/** Commande fournisseur (lot F13). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_purchase_order")
public class AccPurchaseOrder {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "supplier_code", nullable = false)
    private String supplierCode;

    @Column(name = "need_statement_number")
    private String needStatementNumber;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "section_code")
    private String sectionCode;

    @Builder.Default
    @Column(name = "total_ht", nullable = false)
    private BigDecimal totalHt = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_vat", nullable = false)
    private BigDecimal totalVat = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_ttc", nullable = false)
    private BigDecimal totalTtc = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "invoiced_ht", nullable = false)
    private BigDecimal invoicedHt = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "DRAFT";

    @Builder.Default
    @Column(name = "budget_committed", nullable = false)
    private Boolean budgetCommitted = Boolean.FALSE;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
