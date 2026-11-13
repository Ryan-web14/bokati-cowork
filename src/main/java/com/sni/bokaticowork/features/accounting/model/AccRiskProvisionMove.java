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

/** Mouvement de la provision d'un risque : dotation, reprise utilisee ou non, reclassement (lot F19). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_risk_provision_move")
public class AccRiskProvisionMove {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "risk_id")
    private Long riskId;

    @Column(name = "kind")
    private String kind;

    @Column(name = "move_date")
    private LocalDate moveDate;

    @Builder.Default
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "from_account")
    private String fromAccount;

    @Column(name = "to_account")
    private String toAccount;

    @Column(name = "reason")
    private String reason;

    @Column(name = "entry_number")
    private String entryNumber;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
