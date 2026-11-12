package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Regle de ventilation automatique. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_analytic_rule")
public class AccAnalyticRule {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Builder.Default
    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    @Column(name = "axis_code", nullable = false)
    private String axisCode;

    @Column(name = "account_prefix")
    private String accountPrefix;

    @Column(name = "journal_code")
    private String journalCode;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "third_party")
    private String thirdParty;

    @Column(name = "section_code")
    private String sectionCode;

    @Column(name = "key_code")
    private String keyCode;

    @Column(name = "label")
    private String label;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

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
