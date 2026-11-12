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

/** Estimation datee d'un risque (lot F19). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_risk_valuation")
public class AccRiskValuation {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "risk_id")
    private Long riskId;

    @Column(name = "valuation_date")
    private LocalDate valuationDate;

    @Builder.Default
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "probability")
    private String probability;

    @Column(name = "reason")
    private String reason;

    @Column(name = "document_ref")
    private String documentRef;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
