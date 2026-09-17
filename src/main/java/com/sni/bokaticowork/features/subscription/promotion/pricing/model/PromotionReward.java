package com.sni.bokaticowork.features.subscription.promotion.pricing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.RewardType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ce qu'une promotion accorde, et sur quoi.
 *
 * <p>Separer la recompense de la promotion permet d'exprimer « achetez un pass journee, le second
 * est offert » sans ajouter un type d'enumeration a chaque nouvelle idee commerciale. Une promotion
 * peut porter plusieurs recompenses.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "promotion_reward", indexes = {
        @Index(name = "idx_promotion_reward_promotion", columnList = "promotion_id")
})
public class PromotionReward {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 60)
    private RewardType rewardType;

    @Column(name = "value", precision = 19, scale = 4)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_scope", nullable = false, length = 40)
    @Builder.Default
    private TargetScope targetScope = TargetScope.WHOLE_ORDER;

    /** Code de l'objet vise lorsque la portee le demande, nul pour une portee globale. */
    @Column(name = "target_code", length = 120)
    private String targetCode;

    /** Plafond propre a cette recompense, en plus du plafond de la promotion. */
    @Column(name = "max_amount", precision = 19, scale = 4)
    private BigDecimal maxAmount;

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
