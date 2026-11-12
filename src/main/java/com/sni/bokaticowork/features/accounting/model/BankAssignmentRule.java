package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Regle d'affectation d'une ligne de releve sans ecriture. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "bank_assignment_rule")
public class BankAssignmentRule {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "label_pattern", nullable = false)
    private String labelPattern;

    @Column(name = "direction")
    private String direction;

    @Column(name = "min_amount")
    private java.math.BigDecimal minAmount;

    @Column(name = "max_amount")
    private java.math.BigDecimal maxAmount;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "third_party")
    private String thirdParty;

    @Builder.Default
    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

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
