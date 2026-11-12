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

/** Ligne d'ecriture rapprochee dans un lettrage, avec le montant rapproche. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_lettering_line")
public class AccLetteringLine {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "lettering_id", nullable = false)
    private Long letteringId;

    @Column(name = "entry_line_id", nullable = false)
    private Long entryLineId;

    @Column(name = "matched_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal matchedAmount;
}
