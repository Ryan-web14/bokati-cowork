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

/** Interets d'un compte courant d'associe sur une periode (lot F17). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_partner_interest")
public class AccPartnerInterest {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "partner_code", nullable = false)
    private String partnerCode;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "average_balance", nullable = false)
    private BigDecimal averageBalance;

    @Column(name = "rate", nullable = false)
    private BigDecimal rate;

    @Column(name = "interest", nullable = false)
    private BigDecimal interest;

    @Builder.Default
    @Column(name = "non_deductible", nullable = false)
    private BigDecimal nonDeductible = BigDecimal.ZERO;

    @Column(name = "entry_number")
    private String entryNumber;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
