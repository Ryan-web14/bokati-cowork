package com.sni.bokaticowork.features.subscription.derivation.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Au-dela d'un rabais, un visa · au-dela d'un autre, un refus.
 *
 * <p>Le meme modele que les achats et les ajustements de stock : des seuils qui sont des donnees.
 * Le prix plancher d'une version de catalogue s'ajoute a ces seuils et prime sur eux · en dessous,
 * aucune derivation, meme approuvee.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "plan_derivation_approval_rule")
public class PlanDerivationApprovalRule {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "rule_code", nullable = false, unique = true, length = 60)
    private String ruleCode;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "max_discount_percent_without_approval", nullable = false, precision = 9, scale = 4)
    @Builder.Default
    private BigDecimal maxDiscountPercentWithoutApproval = BigDecimal.TEN;

    @Column(name = "max_impact_without_approval", precision = 19, scale = 4)
    private BigDecimal maxImpactWithoutApproval;

    /** Au-dela, refuse meme avec un visa · nul si seul le plancher borne. */
    @Column(name = "max_discount_percent_allowed", precision = 9, scale = 4)
    private BigDecimal maxDiscountPercentAllowed;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
