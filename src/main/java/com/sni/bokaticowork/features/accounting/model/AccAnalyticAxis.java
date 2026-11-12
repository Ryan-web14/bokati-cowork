package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Axe analytique (lot F9). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_analytic_axis")
public class AccAnalyticAxis {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Builder.Default
    @Column(name = "required_on", nullable = false)
    private String requiredOn = "NONE";

    @Column(name = "default_section")
    private String defaultSection;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
