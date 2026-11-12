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

/** Lettrage : lignes d'un compte lettrable qui se compensent. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_lettering")
public class AccLettering {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "third_party_code", length = 120)
    private String thirdPartyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private com.sni.bokaticowork.features.accounting.enums.LetteringKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.accounting.enums.LetteringStatus status;

    @Column(name = "total_debit", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDebit;

    @Column(name = "total_credit", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCredit;

    @Builder.Default
    @Column(name = "difference", nullable = false, precision = 19, scale = 4)
    private BigDecimal difference = BigDecimal.ZERO;

    @Column(name = "difference_entry_number", length = 60)
    private String differenceEntryNumber;

    @Column(name = "lettered_by", nullable = false, length = 180)
    private String letteredBy;

    @Column(name = "unlettered_at")
    private Instant unletteredAt;

    @Column(name = "unlettered_by", length = 180)
    private String unletteredBy;

    @Column(name = "unlettering_reason", length = 500)
    private String unletteringReason;

    @Column(name = "lettered_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
