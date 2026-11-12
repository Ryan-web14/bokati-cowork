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

/** Resultat d'un controle de continuite des numeros. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_numbering_check")
public class AccNumberingCheck {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "journal_code", nullable = false, length = 30)
    private String journalCode;

    @Column(name = "fiscal_year_code", nullable = false, length = 10)
    private String fiscalYearCode;

    @Column(name = "checked_by", length = 180)
    private String checkedBy;

    @Column(name = "first_number")
    private Integer firstNumber;

    @Column(name = "last_number")
    private Integer lastNumber;

    @Column(name = "entry_count", nullable = false)
    private Integer entryCount;

    @Column(name = "counter_value")
    private Integer counterValue;

    @Column(name = "gaps", columnDefinition = "TEXT")
    private String gaps;

    @Column(name = "duplicates", columnDefinition = "TEXT")
    private String duplicates;

    @Column(name = "chain_ok", nullable = false)
    private Boolean chainOk;

    @Column(name = "ok", nullable = false)
    private Boolean ok;

    @Column(name = "checked_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
