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

/** Facture a etablir ou non parvenue, estimee puis extournee (lot F20). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_accrual")
public class AccAccrual {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "kind")
    private String kind;

    @Builder.Default
    @Column(name = "source_type")
    private String sourceType = "MANUAL";

    @Column(name = "reference")
    private String reference;

    @Column(name = "third_party")
    private String thirdParty;

    @Column(name = "label")
    private String label;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "section_code")
    private String sectionCode;

    @Column(name = "period_code")
    private String periodCode;

    @Builder.Default
    @Column(name = "estimated_amount")
    private BigDecimal estimatedAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "vat_amount")
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "status")
    private String status = "PROPOSED";

    @Column(name = "entry_number")
    private String entryNumber;

    @Column(name = "reversal_entry_number")
    private String reversalEntryNumber;

    @Column(name = "actual_reference")
    private String actualReference;

    @Column(name = "actual_amount")
    private BigDecimal actualAmount;

    @Column(name = "gap")
    private BigDecimal gap;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
