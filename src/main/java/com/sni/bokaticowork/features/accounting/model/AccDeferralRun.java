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

/** Traitement de fin de periode des constatations d'avance (lot F20). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_deferral_run")
public class AccDeferralRun {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "period_code")
    private String periodCode;

    @Column(name = "kind")
    private String kind;

    @Builder.Default
    @Column(name = "status")
    private String status = "POSTED";

    @Column(name = "entry_number")
    private String entryNumber;

    @Builder.Default
    @Column(name = "total_need")
    private BigDecimal totalNeed = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_previous")
    private BigDecimal totalPrevious = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "difference")
    private BigDecimal difference = BigDecimal.ZERO;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
