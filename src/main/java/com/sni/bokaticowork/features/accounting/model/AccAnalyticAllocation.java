package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ventilation d'une ligne d'ecriture sur une section. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_analytic_allocation")
public class AccAnalyticAllocation {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "entry_line_id", nullable = false)
    private Long entryLineId;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "axis_code", nullable = false)
    private String axisCode;

    @Column(name = "section_code", nullable = false)
    private String sectionCode;

    @Column(name = "percentage", nullable = false)
    private java.math.BigDecimal percentage;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @Column(name = "origin", nullable = false)
    private String origin;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
