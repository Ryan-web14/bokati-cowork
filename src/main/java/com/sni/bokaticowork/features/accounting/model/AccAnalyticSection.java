package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Section d'un axe ; les postes de depense sont les sections de l'axe POSTE (lot F9). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_analytic_section")
public class AccAnalyticSection {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "axis_code", nullable = false)
    private String axisCode;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "parent_code")
    private String parentCode;

    @Column(name = "family")
    private String family;

    @Column(name = "cost_behavior")
    private String costBehavior;

    @Builder.Default
    @Column(name = "auxiliary", nullable = false)
    private Boolean auxiliary = Boolean.FALSE;

    @Column(name = "responsible")
    private String responsible;

    @Column(name = "valid_from")
    private java.time.LocalDate validFrom;

    @Column(name = "valid_to")
    private java.time.LocalDate validTo;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "merged_into")
    private String mergedInto;

    @Column(name = "color")
    private String color;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.time.Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
        updatedAt = java.time.Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = java.time.Instant.now();
    }
}
