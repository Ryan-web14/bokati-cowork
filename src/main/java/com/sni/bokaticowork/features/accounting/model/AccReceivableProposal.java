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

/** Proposition de classement d'une facture, dans la file de revue (lot F19). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_receivable_proposal")
public class AccReceivableProposal {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "customer_code")
    private String customerCode;

    @Column(name = "trigger_code")
    private String triggerCode;

    @Builder.Default
    @Column(name = "target_status")
    private String targetStatus = "DOUBTFUL";

    @Builder.Default
    @Column(name = "days_overdue")
    private Integer daysOverdue = 0;

    @Builder.Default
    @Column(name = "balance_due")
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "guarantees")
    private BigDecimal guarantees = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "suggested")
    private BigDecimal suggested = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "status")
    private String status = "PROPOSED";

    @Column(name = "postponed_until")
    private LocalDate postponedUntil;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "reason")
    private String reason;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
