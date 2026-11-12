package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Repartition secondaire d'un centre auxiliaire. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_analytic_distribution")
public class AccAnalyticDistribution {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "period_code", nullable = false)
    private String periodCode;

    @Column(name = "axis_code", nullable = false)
    private String axisCode;

    @Column(name = "from_section", nullable = false)
    private String fromSection;

    @Column(name = "to_section", nullable = false)
    private String toSection;

    @Column(name = "key_code", nullable = false)
    private String keyCode;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
