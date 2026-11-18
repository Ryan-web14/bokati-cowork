package com.sni.bokaticowork.features.ressource.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Lot R1 : version d'une regle tarifaire. Chaque creation, modification, activation, desactivation
 * ou suppression garde une photo complete de la regle : un prix facture reste explicable apres coup.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource_pricing_rule_version")
public class ResourcePricingRuleVersion {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    /** Numero court de la regle (celui de l'API). */
    @Column(name = "rule_short_id", nullable = false)
    private Long ruleShortId;

    @Column(name = "resource_code", length = 80)
    private String resourceCode;

    @Column(name = "version", nullable = false)
    private Integer version;

    /** CREATE, UPDATE, ACTIVATE, DEACTIVATE, DELETE. */
    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;

    /** Regle complete apres le changement, en JSON. */
    @Column(name = "snapshot", columnDefinition = "TEXT")
    private String snapshot;

    @Column(name = "changed_by", length = 150)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
