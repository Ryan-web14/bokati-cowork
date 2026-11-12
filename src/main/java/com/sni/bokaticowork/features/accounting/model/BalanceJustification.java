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

/** Justificatif du solde d'un compte ou d'un tiers a une date : pieces ouvertes, ecart, preparation et revue (lot F12). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "balance_justification")
public class BalanceJustification {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "third_party_code")
    private String thirdPartyCode;

    @Column(name = "justified_at")
    private LocalDate justifiedAt;

    @Column(name = "open_items")
    private String openItems;

    @Column(name = "total_open")
    private BigDecimal totalOpen;

    @Column(name = "book_balance")
    private BigDecimal bookBalance;

    @Column(name = "difference")
    private BigDecimal difference;

    @Column(name = "comment")
    private String comment;

    @Column(name = "prepared_by")
    private String preparedBy;

    @Builder.Default
    @Column(name = "prepared_at")
    private Instant preparedAt = Instant.now();

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
