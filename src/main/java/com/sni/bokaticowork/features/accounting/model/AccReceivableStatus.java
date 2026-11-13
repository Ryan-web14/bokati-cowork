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

/** Etat comptable d'une facture client (lot F19) : compte ou elle se trouve, classement, depreciation. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_receivable_status")
public class AccReceivableStatus {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "customer_code")
    private String customerCode;

    @Column(name = "customer_type")
    private String customerType;

    @Builder.Default
    @Column(name = "status")
    private String status = "NORMAL";

    @Builder.Default
    @Column(name = "account_number")
    private String accountNumber = "411";

    @Column(name = "since")
    private LocalDate since;

    @Column(name = "reason")
    private String reason;

    @Column(name = "trigger_code")
    private String triggerCode;

    @Column(name = "next_review_at")
    private LocalDate nextReviewAt;

    @Column(name = "reclass_entry")
    private String reclassEntry;

    @Column(name = "voucher_number")
    private String voucherNumber;

    @Builder.Default
    @Column(name = "amount_ttc")
    private BigDecimal amountTtc = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "impairment")
    private BigDecimal impairment = BigDecimal.ZERO;

    @Column(name = "individual_rate")
    private BigDecimal individualRate;

    @Column(name = "rate_reason")
    private String rateReason;

    @Builder.Default
    @Column(name = "deductible")
    private Boolean deductible = Boolean.TRUE;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}
