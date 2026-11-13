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

/** Exercice comptable. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_fiscal_year")
public class AccFiscalYear {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Builder.Default
    @Column(name = "company_code", nullable = false, length = 40)
    private String companyCode = "MAIN";

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.accounting.enums.FiscalYearStatus status;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by", length = 180)
    private String closedBy;

    /** Resultat net arrete a la cloture (lot F12). */
    @Column(name = "net_result")
    private java.math.BigDecimal netResult;

    /** Piece d'a-nouveaux de l'exercice suivant (lot F12). */
    @Column(name = "opening_entry_number", length = 40)
    private String openingEntryNumber;

    /** Motif de la derogation du responsable financier, si une etape a ete levee (lot F12). */
    @Column(name = "closing_derogation")
    private String closingDerogation;
}
