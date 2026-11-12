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

/** Compte courant d'associe et sa convention (lot F17). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_partner_account")
public class AccPartnerAccount {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "partner_name", nullable = false)
    private String partnerName;

    @Column(name = "third_party_code", nullable = false)
    private String thirdPartyCode;

    @Builder.Default
    @Column(name = "account_number", nullable = false)
    private String accountNumber = "462";

    @Builder.Default
    @Column(name = "blocked", nullable = false)
    private Boolean blocked = Boolean.FALSE;

    @Column(name = "blocked_until")
    private LocalDate blockedUntil;

    @Builder.Default
    @Column(name = "rate", nullable = false)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "admitted_rate")
    private BigDecimal admittedRate;

    @Column(name = "cap")
    private BigDecimal cap;

    @Column(name = "opened_on", nullable = false)
    private LocalDate openedOn;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
