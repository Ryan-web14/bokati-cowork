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

/** Ligne de depreciation d'une facture pour une periode, ou sa reprise (lot F19). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_receivable_impairment")
public class AccReceivableImpairment {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "period_code")
    private String periodCode;

    @Builder.Default
    @Column(name = "base_ht")
    private BigDecimal baseHt = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "guarantees")
    private BigDecimal guarantees = BigDecimal.ZERO;

    @Column(name = "method")
    private String method;

    @Builder.Default
    @Column(name = "rate")
    private BigDecimal rate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "previous_amount")
    private BigDecimal previousAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "difference")
    private BigDecimal difference = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "deductible")
    private Boolean deductible = Boolean.TRUE;

    @Column(name = "override_reason")
    private String overrideReason;

    @Builder.Default
    @Column(name = "kind")
    private String kind = "ADJUSTMENT";

    @Column(name = "entry_number")
    private String entryNumber;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
