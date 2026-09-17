package com.sni.bokaticowork.features.subscription.promotion.pricing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.ConditionOperator;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.ConditionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Une condition d'application, exprimee en donnees.
 *
 * <p>Une promotion utile est rarement « dix pour cent sur tout ». C'est « dix pour cent sur le
 * premier mois, pour un nouveau membre, sur un plan mensuel ». Tant que cela s'ecrit en code,
 * chaque idee commerciale devient une livraison.</p>
 *
 * <p>Toutes les conditions d'une promotion doivent etre vraies. Un « ou » se modelise par deux
 * promotions, ce qui reste plus lisible qu'un arbre booleen dans une table.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "promotion_condition", indexes = {
        @Index(name = "idx_promotion_condition_promotion", columnList = "promotion_id")
})
public class PromotionCondition {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 60)
    private ConditionType conditionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 30)
    private ConditionOperator operator;

    @Column(name = "value", length = 255)
    private String value;

    /** Valeurs separees par des virgules, pour IN, NOT_IN et BETWEEN. */
    @Column(name = "value_list", columnDefinition = "text")
    private String valueList;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Valeurs de {@code valueList}, nettoyees et sans element vide. */
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
