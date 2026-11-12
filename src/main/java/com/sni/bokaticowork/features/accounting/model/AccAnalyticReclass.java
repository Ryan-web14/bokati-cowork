package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reclassement analytique trace. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_analytic_reclass")
public class AccAnalyticReclass {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "entry_line_id", nullable = false)
    private Long entryLineId;

    @Column(name = "axis_code", nullable = false)
    private String axisCode;

    @Column(name = "from_section", nullable = false)
    private String fromSection;

    @Column(name = "to_section", nullable = false)
    private String toSection;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "entry_number")
    private String entryNumber;

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
