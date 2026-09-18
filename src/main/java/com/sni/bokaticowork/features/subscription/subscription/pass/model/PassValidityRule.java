package com.sni.bokaticowork.features.subscription.subscription.pass.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassValidityRuleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Une restriction d'usage, exprimee en donnees.
 *
 * <p>Un pass valable en semaine seulement, un pass heures creuses, un pass limite a un site, un pass
 * bloque les jours feries, un pass a une visite par jour. Aucun de ces cas ne devrait demander une
 * livraison, et chacun l'aurait demandee tant que la regle vivait dans le code.</p>
 *
 * <p>Une regle porte sur un pass ou sur un plan. Celles du plan valent pour tous les pass qui en
 * sont issus ; celle d'un pass ne vaut que pour lui, et c'est ainsi qu'on accorde une derogation
 * sans toucher au catalogue.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "pass_validity_rule", indexes = {
        @Index(name = "idx_pass_validity_rule_pass", columnList = "pass_id"),
        @Index(name = "idx_pass_validity_rule_version", columnList = "pass_version_id")
})
public class PassValidityRule {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pass_id")
    private Pass pass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pass_version_id")
    private PassPlanVersion passVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 40)
    private PassValidityRuleType ruleType;

    @Column(name = "value", length = 255)
    private String value;

    @Column(name = "value_list", columnDefinition = "text")
    private String valueList;

    /** Vrai la regle autorise, faux elle interdit. Un jour ferie s'exprime par une interdiction. */
    @Column(name = "allow_rule", nullable = false)
    @Builder.Default
    private Boolean allowRule = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public List<String> values() {
        if (valueList == null || valueList.isBlank()) {
            return List.of();
        }
        return Arrays.stream(valueList.split(","))
                .map(String::trim)
                .filter(candidate -> !candidate.isEmpty())
                .toList();
    }

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
