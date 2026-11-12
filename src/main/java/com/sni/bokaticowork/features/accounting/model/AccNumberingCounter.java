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

/** Compteur de numerotation d'un journal pour un exercice (ou une periode). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_numbering_counter")
public class AccNumberingCounter {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "journal_code", nullable = false, length = 30)
    private String journalCode;

    @Column(name = "fiscal_year_code", nullable = false, length = 10)
    private String fiscalYearCode;

    @Builder.Default
    @Column(name = "period_code", nullable = false, length = 10)
    private String periodCode = "";

    @Builder.Default
    @Column(name = "last_number", nullable = false)
    private Integer lastNumber = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
