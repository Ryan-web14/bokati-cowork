package com.sni.bokaticowork.features.accounting.model;

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
import java.time.LocalDate;

/** Decision de passage en perte (DPP) d'une ou plusieurs factures (lot F19). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_receivable_writeoff")
public class AccReceivableWriteoff {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "voucher_number")
    private String voucherNumber;

    @Column(name = "customer_code")
    private String customerCode;

    @Column(name = "document_numbers")
    private String documentNumbers;

    @Builder.Default
    @Column(name = "amount_ht")
    private BigDecimal amountHt = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "amount_vat")
    private BigDecimal amountVat = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "amount_ttc")
    private BigDecimal amountTtc = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "impairment_released")
    private BigDecimal impairmentReleased = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "vat_recovered")
    private Boolean vatRecovered = Boolean.TRUE;

    @Column(name = "reason")
    private String reason;

    @Column(name = "comment")
    private String comment;

    @Column(name = "justification_refs")
    private String justificationRefs;

    @Builder.Default
    @Column(name = "origin")
    private String origin = "DECISION";

    @Builder.Default
    @Column(name = "status")
    private String status = "PREPARED";

    @Column(name = "approval_level")
    private String approvalLevel;

    @Column(name = "prepared_by")
    private String preparedBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "entry_number")
    private String entryNumber;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
