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

/** Revue de cloture des provisions et depreciations, PV RVP (lot F19). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_provision_review")
public class AccProvisionReview {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "fiscal_year")
    private String fiscalYear;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "voucher_number")
    private String voucherNumber;

    @Column(name = "summary")
    private String summary;

    @Builder.Default
    @Column(name = "status")
    private String status = "PREPARED";

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
